
namespace Tutorial2
{
    using Microsoft.Research.Malmo;
    using System;
    using System.Collections.Generic;
    using System.IO;
    using System.Linq;
    using System.Reflection;
    using System.Text;
    using System.Threading;
    using System.Threading.Tasks;

    class Program
    {
        public static void Main()
        {
            AgentHost agentHost = new AgentHost();
            try
            {
                agentHost.parse(new StringVector(Environment.GetCommandLineArgs()));
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("ERROR: {0}", ex.Message);
                Console.Error.WriteLine(agentHost.getUsage());
                Environment.Exit(1);
            }
            if (agentHost.receivedArgument("help"))
            {
                Console.Error.WriteLine(agentHost.getUsage());
                Environment.Exit(0);
            }


            var currentPath = Path.GetDirectoryName(Assembly.GetExecutingAssembly().Location);
            var missionFilename = Path.Combine(currentPath, "mission.xml");
            var missionString = System.IO.File.ReadAllText(missionFilename);

            MissionSpec mission = new MissionSpec(missionString, validate: true);

            MissionRecordSpec missionRecord = new MissionRecordSpec("./saved_data.tgz");
            missionRecord.recordCommands();
            missionRecord.recordMP4(20, 400000);
            missionRecord.recordRewards();
            missionRecord.recordObservations();

            try
            {
                agentHost.startMission(mission, missionRecord);
            }
            catch (Exception ex)
            {
                Console.Error.WriteLine("Error starting mission: {0}", ex.Message);
                Environment.Exit(1);
            }

            WorldState worldState;

            Console.WriteLine("Waiting for the mission to start");
            do
            {
                Console.Write(".");
                Thread.Sleep(100);
                worldState = agentHost.getWorldState();

                foreach (TimestampedString error in worldState.errors) Console.Error.WriteLine("Error: {0}", error.text);
            }
            while (!worldState.is_mission_running);

            Console.WriteLine();

            Random rand = new Random();
            // main loop:
            agentHost.sendCommand("move 1");
            agentHost.sendCommand("jump 1");
            agentHost.sendCommand("turn -0.5");
            do
            {
                Thread.Sleep(500);
                worldState = agentHost.getWorldState();
                Console.WriteLine(
                    "video,observations,rewards received: {0}, {1}, {2}",
                    worldState.number_of_video_frames_since_last_state,
                    worldState.number_of_observations_since_last_state,
                    worldState.number_of_rewards_since_last_state);
                foreach (TimestampedReward reward in worldState.rewards) Console.Error.WriteLine("Summed reward: {0}", reward.getValue());
                foreach (TimestampedString error in worldState.errors) Console.Error.WriteLine("Error: {0}", error.text);
            }
            while (worldState.is_mission_running);

            Console.WriteLine("Mission has stopped.");
        }
    }
}
