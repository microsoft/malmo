package main

import (
	"fmt"
	"malmo"
	"math"
	"os"
	"time"

	"github.com/satori/go.uuid"
)

const (
	WIDTH  = 860
	HEIGHT = 480
)

func main() {

	// Clear previous failed frames files:
	FAILED_FRAME_DIR := "VisibilityTest_FailedFrames"
	os.RemoveAll(FAILED_FRAME_DIR)

	// Create somewhere to store any failed frames:
	err := os.MkdirAll(FAILED_FRAME_DIR, 0777)
	if err != nil {
		fmt.Printf("cannot create %s directory:\n%v\n", FAILED_FRAME_DIR, err)
		return
	}

	// Create the first agent host - needed to process command-line options:
	master := malmo.NewAgentHost()
	defer master.Free()

	// Parse the command-line options:
	master.AddOptionalFlag("debug,d", "Display mission start debug information.")
	master.AddOptionalFlag("gui,g", "Display image processing steps in a gui window.")
	master.AddOptionalIntArgument("agents,n", "Number of agents to use.", 4)
	err = master.Parse(os.Args)
	if err != nil {
		fmt.Println(err)
		return
	}
	if master.ReceivedArgument("help") {
		fmt.Println(master.GetUsage())
		return
	}

	// Flags:
	DEBUG := master.ReceivedArgument("debug")
	//SHOW_GUI := master.ReceivedArgument("gui")
	INTEGRATION_TEST_MODE := master.ReceivedArgument("test")
	agents_requested := master.GetIntArgument("agents")
	NUM_AGENTS := Imax(2, Imin(agents_requested, 4))
	if NUM_AGENTS != agents_requested {
		fmt.Printf("WARNING: using %d agents, rather than %d\n", NUM_AGENTS, agents_requested)
	}

	// Create the rest of the agents:
	agent_hosts := make([]*malmo.AgentHost, NUM_AGENTS)
	agent_hosts[0] = master
	agent_hosts[0].SetDebugOutput(DEBUG)
	for i := 1; i < NUM_AGENTS; i++ {
		agent_hosts[i] = malmo.NewAgentHost()
		agent_hosts[i].SetDebugOutput(DEBUG)
		defer agent_hosts[i].Free()
	}

	// Counter:
	failed_frame_count := 0

	// Set up a client pool.
	// IMPORTANT: If ANY of the clients will be on a different machine, then you MUST
	// make sure that any client which can be the server has an IP address that is
	// reachable from other machines - ie DO NOT SIMPLY USE 127.0.0.1!!!!
	// The IP address used in the client pool will be broadcast to other agents who
	// are attempting to find the server - so this will fail for any agents on a
	// different machine.
	client_pool := &malmo.ClientPool{}
	for i := 0; i < NUM_AGENTS; i++ {
		client_pool.Add("127.0.0.1", 10000+i)
	}

	// allocate mission record specification
	my_mission_record := &malmo.MissionRecordSpec{}

	// Keep a count of the frames that failed for each agent.
	failed_frames := make([]int, NUM_AGENTS)

	// If we're running as part of the integration tests, just do ten iterations. Otherwise keep going.
	missions_to_run := 30
	if INTEGRATION_TEST_MODE {
		missions_to_run = 1
	}

	// Run missions
	for mission_no := 0; mission_no < missions_to_run; mission_no++ {

		// Create the mission. Force reset for the first mission, to ensure a clean world. No need for subsequent missions.
		reset := true
		if mission_no > 0 {
			reset = false
		}
		xml := createMissionXML(NUM_AGENTS, WIDTH, HEIGHT, reset)
		my_mission := malmo.NewMissionSpecXML(xml, true)
		defer my_mission.Free()
		fmt.Printf("Running mission #%d\n", mission_no)

		// Generate an experiment ID for this mission.
		// This is used to make sure the right clients join the right servers -
		// if the experiment IDs don't match, the startMission request will be rejected.
		// In practice, if the client pool is only being used by one researcher, there
		// should be little danger of clients joining the wrong experiments, so a static
		// ID would probably suffice, though changing the ID on each mission also catches
		// potential problems with clients and servers getting out of step.
		//
		// Note that, in this sample, the same process is responsible for all calls to startMission,
		// so passing the experiment ID like this is a simple matter. If the agentHosts are distributed
		// across different threads, processes, or machines, a different approach will be required.
		// (Eg generate the IDs procedurally, in a way that is guaranteed to produce the same results
		// for each agentHost independently.)
		experimentID := uuid.NewV4().String()

		// start mission
		retries := 10
		verbose := true
		err := malmo.StartMission(retries, agent_hosts, my_mission, client_pool, my_mission_record, experimentID, verbose)
		if err != nil {
			fmt.Printf("%v\n", err)
			return
		}

		// wait
		time.Sleep(1000 * time.Millisecond)

		// Main mission loop.
		// In this test, all we do is stand still and process our frames.
		timed_out := false
		for !timed_out {
			for i := 0; i < NUM_AGENTS; i++ {
				ah := agent_hosts[i]
				ws := ah.GetWorldState()
				if !ws.IsMissionRunning {
					timed_out = true
				}
				if ws.IsMissionRunning && ws.NumberOfVideoFramesSinceLastState > 0 {
					size := len(ws.VideoFrames)
					frame := ws.VideoFrames[size-1]
					//frame.WritePng(fmt.Sprintf("tmp_agent_visibility_%d.png", mission_no))
					agents_detected := processFrame(int(frame.Width), int(frame.Height), frame.Pixels)
					can_see_agent := agents_detected == NUM_AGENTS-1
					if !can_see_agent {
						// The threshold is not entirely bullet-proof - sometimes there are drawing artifacts that can result in
						// false negatives.
						// So we save a copy of the failing frames for manual inspection:
						//image_failed = Image.frombytes('RGB', (width, height), str(pixels))
						//image_failed.save(FAILED_FRAME_DIR + "/failed_frame_agent_" + str(agent) + "_mission_" + str(mission_count) + "_" + str(failed_frame_count) + ".png")
						failed_frame_count += 1
						failed_frames[i] += 1
					}
				}
			}
		}
		fmt.Println()

		fmt.Printf("Waiting for mission to end ")
		hasEnded := false
		for !hasEnded {
			fmt.Printf(".")
			//time.sleep(0.1)
			for _, ah := range agent_hosts {
				ws := ah.GetWorldState()
				if !ws.IsMissionRunning {
					hasEnded = true
				}
			}
		}
		fmt.Println()
		fmt.Printf("Failed frames: %v\n", failed_frames)
		if INTEGRATION_TEST_MODE && sum(failed_frames) > 0 {
			os.Exit(1) // Test failed - quit.
		}

		// wait
		time.Sleep(2000 * time.Millisecond)

		// force freeing memory
		my_mission.Free()
	}
}

// --- processFrame function ----------------------------------------------------------------------

func processFrame(width, height int, pixels []uint8) (agents_detected int) {
	// Attempt some fairly simple image processing in order to determine how many agents the current agent can "see".
	// We rely on the fact that the other agents jut above the horizon line, which is at the mid-point of the image.
	// With the time set to morning, and the weather set to clear, there should always be a good distinction between
	// background (sky) pixels, and foreground (agent) pixels. A bit of thresholding should suffice to provide a
	// fairly reliable way of counting the visible agents.
	channels := 3 // Following code assumes this to be true.

	// 1. Extract a narrow strip of the middle from the middle up:
	y1 := int(float32(height) * 0.45)
	y2 := int(float32(height) * 0.5)
	if y2 == y1 {
		y1 -= 1
	}
	num_rows := y2 - y1
	middle_strip := pixels[y1*width*channels : y2*width*channels]

	// 2. Convert RGB to luminance. Build up a histogram as we go - this will be useful for finding a threshold point.
	hist := make([]int, 256)
	for col := 0; col < width*channels; col += channels {
		for row := 0; row < num_rows; row++ {
			pix := col + row*width*channels
			lum := uint8(0.2126*float64(middle_strip[pix]) + 0.7152*float64(middle_strip[pix+1]) + 0.0722*float64(middle_strip[pix+2]))
			hist[lum] += 1
			middle_strip[pix] = lum   // assuming channels == 3
			middle_strip[pix+1] = lum // assuming channels == 3
			middle_strip[pix+2] = lum // assuming channels == 3
		}
	}

	// 3. Calculate a suitable threshold, using the Otsu method
	total_pixels := width * num_rows
	total_sum := 0.0
	for t := 0; t < 256; t++ {
		total_sum += float64(t * hist[t])
	}
	sum_background := 0.0
	weight_background := 0.0
	weight_foreground := 0.0
	max_variation := 0.0
	threshold := 0
	for t := 0; t < 256; t++ {
		weight_background += float64(hist[t])
		if weight_background == 0 {
			continue
		}
		weight_foreground = float64(total_pixels) - weight_background
		if weight_foreground == 0 {
			break
		}
		sum_background += float64(t * hist[t])
		mean_background := sum_background / weight_background
		mean_foreground := (total_sum - sum_background) / weight_foreground
		// Between class variance
		vari := weight_background * weight_foreground * (mean_background - mean_foreground) * (mean_background - mean_foreground)
		if vari > max_variation {
			max_variation = vari
			threshold = t
		}
	}

	// 4. Apply this threshold
	for pix := 0; pix < len(middle_strip); pix++ {
		if middle_strip[pix] <= uint8(threshold) {
			middle_strip[pix] = 255
		} else {
			middle_strip[pix] = 0
		}
	}

	// 5. OR together all the rows. This helps to de-noise the image.
	// At the same time, we count the number of changes (from foreground to background, or background to foreground)
	// that occur across the scanline. Assuming that there are no partial agents at the sides of the view - ie the scanline
	// starts and ends with background - this count should result in two changes per visible agent.
	pixelvalue := func(col int) (res uint8) {
		for x := col; x < len(middle_strip); x += width * channels {
			res += middle_strip[x]
		}
		return
	}
	lastval := 255
	changes := 0
	for col := 0; col < width*channels; col += channels {
		val := 255
		if pixelvalue(col) > 0 {
			val = 0
		}
		if lastval != val {
			changes += 1
		}
		lastval = val
	}

	// 6. Return number of detected agents
	agents_detected = changes / 2
	return
}

// --- auxiliary --- malmo ------------------------------------------------------------------------

func getPlacementString(i, num_agents int) string {
	// Place agents at equal points around a circle, facing inwards.
	radius := 5.0
	accuracy := 1000.0
	angle := 2 * float64(i) * math.Pi / float64(num_agents)
	x := int(accuracy*radius*math.Cos(angle)) / int(accuracy)
	y := 227
	z := int(accuracy*radius*math.Sin(angle)) / int(accuracy)
	pitch := 0
	yaw := int(90 + angle*180.0/math.Pi)
	return fmt.Sprintf("x=\"%d\" y=\"%d\" z=\"%d\" pitch=\"%d\" yaw=\"%d\"", x, y, z, pitch, yaw)
}

func createMissionXML(num_agents, width, height int, reset bool) (xml string) {
	// Set up the Mission XML.
	// First, the server section.
	// Weather MUST be set to clear, since the dark thundery sky plays havoc with the image thresholding.
	xml = `<?xml version="1.0" encoding="UTF-8" standalone="no" ?>
    <Mission xmlns="http://ProjectMalmo.microsoft.com" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <About>
        <Summary/>
      </About>
      <ServerSection>
        <ServerInitialConditions>
          <Time>
            <StartTime>1000</StartTime>
          </Time>
          <Weather>clear</Weather>
        </ServerInitialConditions>
        <ServerHandlers>
          <FlatWorldGenerator forceReset="` + fmt.Sprintf("%v", reset) + `" generatorString="3;2*4,225*22;1;" seed=""/>
          <ServerQuitFromTimeUp description="" timeLimitMs="10000"/>
        </ServerHandlers>
      </ServerSection>
    `

	// Add an agent section for each watcher.
	// We put them in a leather helmet because it makes the image processing slightly easier.
	for i := 0; i < num_agents; i++ {
		placement := getPlacementString(i, num_agents)
		xml += `<AgentSection mode="Survival">
        <Name>Watcher#` + fmt.Sprintf("%d", i) + `</Name>
        <AgentStart>
          <Placement ` + placement + `/>
          <Inventory>
            <InventoryObject type="leather_helmet" slot="39" quantity="1"/>
          </Inventory>
        </AgentStart>
        <AgentHandlers>
          <VideoProducer>
            <Width>` + fmt.Sprintf("%d", width) + `</Width>
            <Height>` + fmt.Sprintf("%d", height) + `</Height>
          </VideoProducer>
        </AgentHandlers>
      </AgentSection>`
	}

	xml += "</Mission>"
	return
}

// --- auxiliary --- generic ----------------------------------------------------------------------

// Imin returns the minimum between two integers
func Imin(a, b int) int {
	if a < b {
		return a
	}
	return b
}

// Imin returns the maximum between two integers
func Imax(a, b int) int {
	if a > b {
		return a
	}
	return b
}

func sum(array []int) (res int) {
	for _, v := range array {
		res += v
	}
	return
}
