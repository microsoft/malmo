// --------------------------------------------------------------------------------------------------
//  Copyright (c) 2016 Microsoft Corporation
//  
//  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
//  associated documentation files (the "Software"), to deal in the Software without restriction,
//  including without limitation the rights to use, copy, modify, merge, publish, distribute,
//  sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//  
//  The above copyright notice and this permission notice shall be included in all copies or
//  substantial portions of the Software.
//  
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
//  NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
//  NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
//  DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// --------------------------------------------------------------------------------------------------

namespace Microsoft.Research.Malmo.HumanAction
{
    using System;
    using System.Collections.Generic;
    using System.Diagnostics;
    using System.IO;
    using System.IO.Compression;
    using System.Linq;
    using System.Net;
    using System.Text;
    using System.Threading;
    using System.Threading.Tasks;
    using System.Windows;
    using System.Windows.Controls;
    using System.Windows.Controls.Primitives;
    using System.Windows.Input;
    using System.Windows.Media;
    using System.Windows.Media.Imaging;
    using System.Windows.Threading;
    using System.Xml.Linq;
    using Microsoft.Research.Malmo.HumanAction.Properties;
    using SlimDX.XInput;

    /// <summary>
    /// Interaction logic for MainWindow XAML.
    /// </summary>
    public partial class MainWindow
    {
        /// <summary>
        /// Record Window
        /// </summary>
        private RecordWindow _recordWindow;

        /// <summary>
        /// Whether the human completed the last mission successfully
        /// </summary>
        private bool _missionSuccess;

        /// <summary>
        /// Paths to the curriculum items
        /// </summary>
        private string[] _curriculumItemPaths;

        /// <summary>
        /// Commands since last game loop
        /// </summary>
        private Queue<Tuple<string, float>> _pendingCommandQueue;

        /// <summary>
        /// Time the mission will take
        /// </summary>
        private TimeSpan _missionDuration;

        /// <summary>
        /// Remaining time in the mission
        /// </summary>
        private TimeSpan _timeRemaining;

        /// <summary>
        /// Random number generator
        /// </summary>
        private Random _rand;

        /// <summary>
        /// The high score window
        /// </summary>
        private HighScoreWindow _highScoreWindow;

        /// <summary>
        /// Object representing the state of the gamepad
        /// </summary>
        private GamepadState _gamepad;

        /// <summary>
        /// Whether to stop the current mission
        /// </summary>
        private bool _stopMission;

        /// <summary>
        /// Score value for the current mission
        /// </summary>
        private double _score;

        /// <summary>
        /// Dictionary used to store the previous argument sent for a particular command to avoid spamming commands over the wire.
        /// </summary>
        private Dictionary<string, float> _continuousCommandState;

        /// <summary>
        /// Dictionary used to store the current state of a discrete command to avoid sending multiple messages per key/button press.
        /// </summary>
        private Dictionary<string, bool> _discreteCommandState;

        /// <summary>
        /// Timer controlling communication with the agent host
        /// </summary>
        private DispatcherTimer _timer;

        /// <summary>
        /// Bitmap providing a buffer for video frames
        /// </summary>
        private WriteableBitmap _bitmap;

        /// <summary>
        /// Buffer for pixels.
        /// </summary>
        private byte[] _pixels;

        /// <summary>
        /// True if a session is currently running.
        /// </summary>
        private bool _isInSession;

        /// <summary>
        /// Mutex to protect pending command queue
        /// </summary>
        private SemaphoreSlim _pendingCommandsMutex;

        /// <summary>
        /// Mutex to protect pixels buffer
        /// </summary>
        private SemaphoreSlim _pixelsMutex;

        /// <summary>
        /// Mutex to protect message buffer
        /// </summary>
        private SemaphoreSlim _pendingMessagesMutex;

        /// <summary>
        /// The queue of pending messages
        /// </summary>
        private Queue<string> _pendingMessages;

        /// <summary>
        /// Current sticky verb
        /// </summary>
        private string _resetVerb;

        /// <summary>
        /// Initializes a new instance of the <see cref="MainWindow"/> class.
        /// </summary>
        public MainWindow()
        {
            InitializeComponent();
        }

        /// <summary>
        /// Called when the window is loaded.
        /// </summary>
        /// <param name="sender">The event sender</param>
        /// <param name="e">The arguments</param>
        private void WindowLoaded(object sender, RoutedEventArgs e)
        {
            _bitmap = new WriteableBitmap(Settings.Default.VideoWidth, Settings.Default.VideoHeight, 72, 72, PixelFormats.Rgb24, null);

            VideoImage.Source = _bitmap;

            _isInSession = false;
            Title = Settings.Default.Title + " : Ready";

            _rand = new Random();
            _gamepad = new GamepadState(UserIndex.One);
            _continuousCommandState = new Dictionary<string, float>();

            _curriculumItemPaths = Settings.Default.Curriculum.Split(new char[] { '|' }, StringSplitOptions.RemoveEmptyEntries);

            _continuousCommandState.Add("move", 0);
            _continuousCommandState.Add("strafe", 0);
            _continuousCommandState.Add("pitch", 0);
            _continuousCommandState.Add("turn", 0);
            _continuousCommandState.Add("crouch", 0);
            _continuousCommandState.Add("jump", 0);
            _continuousCommandState.Add("attack", 0);
            _continuousCommandState.Add("use", 0);

            _discreteCommandState = new Dictionary<string, bool>();
            _discreteCommandState.Add("movenorth", false);
            _discreteCommandState.Add("moveeast", false);
            _discreteCommandState.Add("movesouth", false);
            _discreteCommandState.Add("movewest", false);

            _pendingMessagesMutex = new SemaphoreSlim(1);
            _pendingCommandsMutex = new SemaphoreSlim(1);
            _pixelsMutex = new SemaphoreSlim(1);

            _pendingMessages = new Queue<string>();

            _timer = new DispatcherTimer();
            _timer.Interval = TimeSpan.FromSeconds(1.0 / 30);
            _timer.Tick += timer_Tick;
            _timer.Start();
        }

        /// <summary>
        /// Called on each timer tick.
        /// </summary>
        /// <param name="sender">The timer</param>
        /// <param name="e">The event arguments</param>
        void timer_Tick(object sender, EventArgs e)
        {
            if (_isInSession)
            {
                UpdateDisplayedMessages();
                UpdateDisplayedReward();
                UpdateDisplayedFrame();
            }
            else
            {
                if (!_gamepad.IsConnected)
                {
                    return;
                }

                _gamepad.Update();
                if (_gamepad.Start)
                {
                    startB.RaiseEvent(new RoutedEventArgs(ButtonBase.ClickEvent));
                }
            }
        }

        private void UpdateDisplayedMessages()
        {
            _pendingMessagesMutex.Wait();
            try
            {
                if (_recordWindow != null && _recordWindow.IsVisible)
                {
                    _recordWindow.Write(_pendingMessages);
                }

                _pendingMessages.Clear();                
            }
            finally
            {
                _pendingMessagesMutex.Release();
            }
        }

        /// <summary>
        /// Called when a reward message is received from the pipeline.
        /// </summary>
        /// <param name="message">The message</param>
        private void UpdateDisplayedReward()
        {
            scoreTB.Text = string.Format("SCORE {0}", ((int)_score).ToString().PadLeft(Score.ValueLength, '0'));
        }

        /// <summary>
        /// Writes the pixels of the frame to the display buffer in reverse row order.
        /// </summary>
        private unsafe void UpdateDisplayedFrame()
        {
            _pixelsMutex.Wait();

            try {
                if (_pixels == null)
                {
                    return;
                }

                if (_timeRemaining != TimeSpan.Zero)
                {
                    startB.Content = (int)_timeRemaining.TotalSeconds;
                    if (_recordWindow != null && _recordWindow.IsVisible)
                    {
                        _recordWindow.Elapsed = _missionDuration - _timeRemaining;
                    }
                }

                if (_bitmap.TryLock(new Duration(TimeSpan.FromSeconds(1.0 / 60))))
                {
                    fixed (byte* srcBuffer = _pixels)
                    {
                        int srcStride = _bitmap.PixelWidth * 3;
                        int dstStride = _bitmap.BackBufferStride;

                        byte* dstScan = (byte*)_bitmap.BackBuffer;
                        byte* srcScan = srcBuffer;

                        for (int r = 0; r < _bitmap.PixelHeight; r++, srcScan += srcStride, dstScan += dstStride)
                        {
                            byte* srcPtr = srcScan;
                            byte* dstPtr = dstScan;
                            for (int c = 0; c < _bitmap.PixelWidth; c++)
                            {
                                *dstPtr++ = *srcPtr++;
                                *dstPtr++ = *srcPtr++;
                                *dstPtr++ = *srcPtr++;
                            }
                        }
                    }

                    _bitmap.AddDirtyRect(new Int32Rect(0, 0, _bitmap.PixelWidth, _bitmap.PixelHeight));
                    _bitmap.Unlock();
                }
            }
            finally
            {
                _pixelsMutex.Release();
            }
        }
        
        /// <summary>
        /// Called when the window is closing.
        /// </summary>
        /// <param name="sender">The event sender</param>
        /// <param name="e">The arguments</param>
        private void Window_Closing(object sender, System.ComponentModel.CancelEventArgs e)
        {
            if (_isInSession)
            {
                _stopMission = true;
            }

            if (_highScoreWindow != null)
            {
                _highScoreWindow.Close();
            }

            if (_recordWindow != null)
            {
                _recordWindow.Close();
            }
        }

        /// <summary>
        /// Called when a key is pressed.
        /// </summary>
        /// <param name="sender">The event sender</param>
        /// <param name="e">The event arguments</param>
        private async void VideoImage_KeyDown(object sender, System.Windows.Input.KeyEventArgs e)
        {
            e.Handled = true;

            await _pendingCommandsMutex.WaitAsync();
            try {
                if (_pendingCommandQueue == null)
                {
                    return;
                }

                switch (e.Key)
                {
                    case Key.W:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("move", 1));
                        break;

                    case Key.A:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("strafe", -1));
                        break;

                    case Key.S:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("move", -1));
                        break;

                    case Key.D:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("strafe", 1));
                        break;

                    case Key.Up:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("pitch", 1));
                        break;

                    case Key.Down:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("pitch", -1));
                        break;

                    case Key.Left:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("turn", -1));
                        break;

                    case Key.Right:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("turn", 1));
                        break;

                    case Key.Space:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("jump", 1));
                        break;

                    case Key.LeftShift:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("crouch", 1));
                        break;

                    case Key.Enter:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("attack", 1));
                        break;

                    case Key.RightShift:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("use", 1));
                        break;
                }
            }
            finally
            {
                _pendingCommandsMutex.Release();
            }
        }

        /// <summary>
        /// Called when a key is released.
        /// </summary>
        /// <param name="sender">The event sender</param>
        /// <param name="e">The event arguments</param>
        private async void VideoImage_KeyUp(object sender, KeyEventArgs e)
        {
            e.Handled = true;
            if (Settings.Default.StickyKeys)
            {
                return;
            }

            await _pendingCommandsMutex.WaitAsync();
            try {
                if (_pendingCommandQueue == null)
                {
                    return;
                }

                switch (e.Key)
                {
                    case Key.W:
                    case Key.S:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("move", 0));
                        break;

                    case Key.A:
                    case Key.D:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("strafe", 0));
                        break;

                    case Key.Up:
                    case Key.Down:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("pitch", 0));
                        break;

                    case Key.Left:
                    case Key.Right:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("turn", 0));
                        break;

                    case Key.Space:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("jump", 0));
                        break;

                    case Key.LeftShift:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("crouch", 0));
                        break;

                    case Key.Enter:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("attack", 0));
                        break;

                    case Key.RightShift:
                        _pendingCommandQueue.Enqueue(new Tuple<string, float>("use", 0));
                        break;
                }
            }
            finally
            {
                _pendingCommandsMutex.Release();
            }
        }

        private void CheckGamepad(AgentHost agentHost)
        {
            if (!_gamepad.IsConnected)
            {
                return;
            }

            _gamepad.Update();

            CheckAndSend(agentHost, "move", _gamepad.LeftStick.Position.Y);
            CheckAndSend(agentHost, "strafe", _gamepad.LeftStick.Position.X);
            CheckAndSend(agentHost, "pitch", _gamepad.RightStick.Position.Y);
            CheckAndSend(agentHost, "turn", _gamepad.RightStick.Position.X);
            CheckAndSend(agentHost, "crouch", _gamepad.X ? 1 : 0);
            CheckAndSend(agentHost, "jump", _gamepad.A ? 1 : 0);
            CheckAndSend(agentHost, "attack", _gamepad.RightTrigger ? 1 : 0);
            CheckAndSend(agentHost, "use", _gamepad.LeftTrigger ? 1 : 0);
            CheckAndSend(agentHost, "movesouth", _gamepad.DPad.Up ? 1 : 0);
            CheckAndSend(agentHost, "movenorth", _gamepad.DPad.Down ? 1 : 0);
            CheckAndSend(agentHost, "movewest", _gamepad.DPad.Right ? 1 : 0);
            CheckAndSend(agentHost, "moveeast", _gamepad.DPad.Left ? 1 : 0);            
        }

        /// <summary>
        /// Check the previous value for the command verb and only send a message if it has changed.
        /// </summary>
        /// <param name="agentHost">The agent host</param>
        /// <param name="verb">The verb to check</param>
        /// <param name="newValue">The measured value from the gamepad</param>
        private void CheckAndSend(AgentHost agentHost, string verb, float newValue)
        {
            switch (verb)
            {
                case "move":
                    if (!Settings.Default.IsMoveEnabled)
                    {
                        return;
                    }

                    break;

                case "strafe":
                    if (!Settings.Default.IsStrafeEnabled)
                    {
                        return;
                    }

                    break;

                case "pitch":
                    if (!Settings.Default.IsPitchEnabled)
                    {
                        return;
                    }

                    break;

                case "turn":
                    if (!Settings.Default.IsTurnEnabled)
                    {
                        return;
                    }

                    break;

                case "jump":
                    if (!Settings.Default.IsJumpEnabled)
                    {
                        return;
                    }

                    break;

                case "crouch":
                    if (!Settings.Default.IsCrouchEnabled)
                    {
                        return;
                    }

                    break;

                case "attack":
                    if (!Settings.Default.IsAttackEnabled)
                    {
                        return;
                    }

                    break;

                case "use":
                    if (!Settings.Default.IsUseEnabled)
                    {
                        return;
                    }

                    break;

                case "movenorth":
                    if (!Settings.Default.IsNorthEnabled)
                    {
                        return;
                    }

                    break;

                case "moveeast":
                    if (!Settings.Default.IsEastEnabled)
                    {
                        return;
                    }

                    break;

                case "movesouth":
                    if (!Settings.Default.IsSouthEnabled)
                    {
                        return;
                    }

                    break;

                case "movewest":
                    if (!Settings.Default.IsWestEnabled)
                    {
                        return;
                    }

                    break;
            }

            try {
                if (Settings.Default.StickyKeys && !string.IsNullOrEmpty(_resetVerb))
                {
                    string command = string.Format("{0} {1}", _resetVerb, 0);
                    addCommandToQueue(command);
                    agentHost.sendCommand(command);
                    _continuousCommandState[verb] = 0;
                }

                _resetVerb = verb;

                if (_continuousCommandState.ContainsKey(verb))
                {
                    float currentValue = _continuousCommandState[verb];
                    if (newValue != currentValue)
                    {
                        string command = string.Format("{0} {1}", verb, newValue);
                        addCommandToQueue(command);
                        agentHost.sendCommand(command);
                        _continuousCommandState[verb] = newValue;
                    }
                }
                else if(_discreteCommandState.ContainsKey(verb))
                {
                    if (newValue > 0)
                    {
                        if (!_discreteCommandState[verb])
                        {
                            _discreteCommandState[verb] = true;
                            string command = string.Format("{0} 1", verb, newValue);
                            addCommandToQueue(command);
                            agentHost.sendCommand(command);
                        }
                    }
                    else
                    {
                        _discreteCommandState[verb] = false;
                    }
                }
            }
            catch
            {                
            }
        }

        private void addCommandToQueue(string command)
        {
            _pendingMessagesMutex.Wait();
            try
            {
                _pendingMessages.Enqueue(string.Format("{0}> {1}", DateTime.UtcNow.ToString("hh:mm:ss.fff"), command));
            }
            finally
            {
                _pendingMessagesMutex.Release();
            }
        }

        private async void openMI_Click(object sender, RoutedEventArgs e)
        {
            var ofd = new Win32.OpenFileDialog();
            ofd.Filter = "Mission Files (*.xml)|*.xml";

            if (ofd.ShowDialog(this) == true)
            {
                MissionSpec mission = await loadMission(ofd.FileName);
                if (mission != null)
                {
                    _score = 0;

                    openMI.IsEnabled = false;
                    startB.IsEnabled = false;
                    startB.Content = "Ready";
                    await Task.Run(() => runMission(mission));
                    startB.IsEnabled = true;
                    startB.Content = "Start";
                    openMI.IsEnabled = true;
                }
            }
        }

        private async Task<MissionSpec> loadMission(string path)
        {
            using (StreamReader reader = new StreamReader(path))
            {
                try
                {
                    string xml = await reader.ReadToEndAsync();
                    xml = xml.Replace("__SEED__", _rand.Next().ToString());
                    MissionSpec mission = new MissionSpec(xml, true);

                    if (!mission.isVideoRequested(0))
                    {
                        mission.requestVideo(Settings.Default.VideoWidth, Settings.Default.VideoHeight);
                    }

                    mission.removeAllCommandHandlers();
                    mission.allowAllContinuousMovementCommands();
                    mission.allowAllDiscreteMovementCommands();                    

                    XDocument xdoc = XDocument.Parse(xml);
                    XNamespace malmo = "http://ProjectMalmo.microsoft.com";
                    var timeUpNode = xdoc.Descendants(malmo + "ServerQuitFromTimeUp").FirstOrDefault();
                    if(timeUpNode != null)
                    {
                        _missionDuration = TimeSpan.FromMilliseconds(double.Parse(timeUpNode.Attribute("timeLimitMs").Value));
                    }
                    else
                    {
                        _missionDuration = TimeSpan.Zero;
                    }

                    if (_recordWindow != null && _recordWindow.IsVisible)
                    {
                        _recordWindow.MissionPath = path;
                    }

                    return mission;
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message, "Error Parsing Mission", MessageBoxButton.OK, MessageBoxImage.Error);
                    return null;
                }
            }
        }

        private void runMission(MissionSpec mission)
        {
            string recordPath = "none";
            MissionRecordSpec missionRecord;
            if (string.IsNullOrEmpty(Settings.Default.OutputDir))
            {
                missionRecord = new MissionRecordSpec();
            }
            else
            {
                recordPath = Path.Combine(Settings.Default.OutputDir, Guid.NewGuid() + ".tar.gz");
                missionRecord = new MissionRecordSpec(recordPath);
                if (Settings.Default.RecordObservations)
                {
                    missionRecord.recordObservations();
                }

                if (Settings.Default.RecordRewards)
                {
                    missionRecord.recordRewards();
                }

                if (Settings.Default.RecordCommands)
                {
                    missionRecord.recordCommands();
                }

                if (Settings.Default.RecordMP4)
                {
                    missionRecord.recordMP4(Settings.Default.MP4FPS, (long)(Settings.Default.BitRate * 1024));
                }
            }

            using (AgentHost agentHost = new AgentHost())
            {
                ClientPool clientPool = new ClientPool();
                clientPool.add(new ClientInfo(Settings.Default.ClientIP, Settings.Default.ClientPort));

                try
                {
                    agentHost.startMission(mission, clientPool, missionRecord, 0, "hac");
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message, "Error Starting Mission", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }

                Dispatcher.Invoke(() =>
                {
                    Activate();
                    VideoImage.Focus();
                    startB.Content = "Set";
                    Title = Settings.Default.Title + " : Waiting for mission start";
                    if (_recordWindow != null && _recordWindow.IsVisible)
                    {
                        _recordWindow.RecordPath = recordPath;
                    }
                });

                _isInSession = true;
                try
                {
                    WorldState worldState;
                    // wait for mission to start
                    do
                    {
                        Thread.Sleep(100);
                        worldState = agentHost.getWorldState();

                        if (worldState.errors.Any())
                        {
                            StringBuilder errors = new StringBuilder();
                            foreach (TimestampedString error in worldState.errors)
                            {
                                errors.AppendLine(error.text);
                            }

                            MessageBox.Show(errors.ToString(), "Error during mission initialization", MessageBoxButton.OK, MessageBoxImage.Error);
                            return;
                        }
                    }
                    while (!worldState.is_mission_running && !_stopMission);

                    if (_stopMission)
                    {
                        return;
                    }

                    DateTime missionStartTime = DateTime.UtcNow;
                    _timeRemaining = _missionDuration;

                    Dispatcher.Invoke(() =>
                    {
                        startB.Content = "Go!";
                        Title = Settings.Default.Title + " : Recording";
                    });

                    // run mission
                    TimeSpan loopTime = TimeSpan.FromSeconds(1.0 / 20);

                    _pendingCommandsMutex.Wait();
                    try
                    {
                        _pendingCommandQueue = new Queue<Tuple<string, float>>();
                    }
                    finally
                    {
                        _pendingCommandsMutex.Release();
                    }

                    _pendingMessagesMutex.Wait();
                    try
                    {
                        _pendingMessages = new Queue<string>();
                    }
                    finally
                    {
                        _pendingMessagesMutex.Release();
                    }

                    bool failure = false;
                    Stopwatch loopTimer = new Stopwatch();
                    do
                    {
                        loopTimer.Reset();
                        loopTimer.Start();

                        worldState = agentHost.getWorldState();

                        TimestampedVideoFrame frame = worldState.video_frames.FirstOrDefault();

                        if (frame != null)
                        {
                            if (_missionDuration != TimeSpan.Zero)
                            {
                                TimeSpan elapsed = frame.timestamp - missionStartTime;
                                _timeRemaining = _missionDuration - elapsed;
                            }

                            _pixelsMutex.Wait();
                            try
                            {
                                if (_pixels == null || _pixels.Length != frame.pixels.Count)
                                {
                                    _pixels = new byte[frame.pixels.Count];
                                    Dispatcher.Invoke(() =>
                                    {
                                        if (_bitmap.Width != frame.width || _bitmap.Height != frame.height)
                                        {
                                            _bitmap = new WriteableBitmap(frame.width, frame.height, 72, 72, PixelFormats.Rgb24, null);
                                            VideoImage.Source = _bitmap;
                                            if (_recordWindow != null && _recordWindow.IsVisible)
                                            {
                                                _recordWindow.FrameSource = _bitmap;
                                            }
                                        }
                                    });
                                }

                                frame.pixels.CopyTo(_pixels);
                            }
                            finally
                            {
                                _pixelsMutex.Release();
                            }
                        }

                        _pendingMessagesMutex.Wait();
                        try
                        {
                            foreach (var reward in worldState.rewards)
                            {
                                _score += reward.getValue();
                                if (reward.getValue() < 0)
                                {
                                    failure = true;
                                }

                                _pendingMessages.Enqueue(string.Format("{0}> score {1}", reward.timestamp.ToString("hh:mm:ss.fff"), reward.getValue()));
                            }

                            _score = Math.Max(_score, 0);
                            _score = Math.Min(_score, 99999);

                            foreach (var observation in worldState.observations)
                            {
                                int posStart = observation.text.IndexOf("\"XPos\"");
                                if (posStart < 0)
                                {
                                    continue;
                                }

                                int posEnd = observation.text.IndexOf("\"ZPos\"");
                                posEnd = observation.text.IndexOf(',', posEnd);

                                string posSegment = observation.text.Substring(posStart, posEnd - posStart);
                                string[] pos = posSegment.Split(',');
                                float x = Convert.ToSingle(pos[0].Split(':')[1]);
                                float y = Convert.ToSingle(pos[1].Split(':')[1]);
                                float z = Convert.ToSingle(pos[2].Split(':')[1]);

                                _pendingMessages.Enqueue(string.Format("{0}> (x={1:0.00}, y={2:0.00}, z={3:0.00})", observation.timestamp.ToString("hh:mm:ss.fff"), x, y, z));
                            }
                        }
                        finally
                        {
                            _pendingMessagesMutex.Release();
                        }

                        CheckGamepad(agentHost);

                        _pendingCommandsMutex.Wait();
                        try
                        {
                            while (_pendingCommandQueue.Any())
                            {
                                var command = _pendingCommandQueue.Dequeue();
                                CheckAndSend(agentHost, command.Item1, command.Item2);
                            }
                        }
                        finally
                        {
                            _pendingCommandsMutex.Release();
                        }

                        loopTimer.Stop();
                        if (loopTimer.Elapsed < loopTime)
                        {
                            Thread.Sleep(loopTime - loopTimer.Elapsed);
                        }

                    } while (worldState.is_mission_running && !_stopMission);

                    if (_stopMission)
                    {
                        return;
                    }

                    if (!failure)
                    {
                        _score += _timeRemaining.TotalSeconds * 100;
                    }

                    _missionSuccess = !failure;

                    _pendingCommandsMutex.Wait();
                    try
                    {
                        _pendingCommandQueue = null;
                    }
                    finally
                    {
                        _pendingCommandsMutex.Release();
                    }

                    Dispatcher.Invoke(() =>
                    {
                        Title = Settings.Default.Title + " : Ready";
                        UpdateDisplayedMessages();
                        UpdateDisplayedReward();
                    });

                    _resetVerb = null;

                    var keys = _continuousCommandState.Keys.ToList();
                    foreach (var verb in keys)
                    {
                        _continuousCommandState[verb] = 0;
                    }

                    keys = _discreteCommandState.Keys.ToList();
                    foreach (var verb in keys)
                    {
                        _discreteCommandState[verb] = false;
                    }

                    if (worldState.errors.Any())
                    {
                        StringBuilder errors = new StringBuilder();
                        foreach (TimestampedString error in worldState.errors)
                        {
                            errors.AppendLine(error.text);
                        }

                        MessageBox.Show(errors.ToString(), "Error during mission initialization", MessageBoxButton.OK, MessageBoxImage.Error);
                        return;
                    }
                }
                catch (Exception ex)
                {
                    MessageBox.Show(ex.Message, "Error during mission", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }
                finally
                {
                    _isInSession = false;
                }
            }
        }    

        private void exitMI_Click(object sender, RoutedEventArgs e)
        {
            App.Current.Shutdown();
        }

        private void preferencesMI_Click(object sender, RoutedEventArgs e)
        {
            PreferencesDialog prefs = new PreferencesDialog();
            prefs.ShowDialog();
        }

        private void curriculumMI_Click(object sender, RoutedEventArgs e)
        {
            CurriculumDialog curr = new CurriculumDialog();
            if (curr.ShowDialog() == true)
            {
                _curriculumItemPaths = curr.Items.ToArray();
            }
        }

        private async void startB_Click(object sender, RoutedEventArgs e)
        {
            if (!startB.IsEnabled)
            {
                return;
            }

            startB.IsEnabled = false;

            _score = 0;
            int index = 0;

            for(int i=0; i<_curriculumItemPaths.Length; i++)
            {
                startB.Content = "Ready";
                MissionSpec spec = await loadMission(_curriculumItemPaths[index]);
                if (spec != null)
                {
                    await Task.Run(() => runMission(spec));
                }

                if (_missionSuccess)
                {
                    index++;
                }
            }

            if (_highScoreWindow != null && _highScoreWindow.IsVisible)
            {
                _highScoreWindow.AddScore((int)_score);
            }
                       
            startB.IsEnabled = true;
            startB.Content = "Start";
        }

        private void highScoreMI_Click(object sender, RoutedEventArgs e)
        {
            if (_highScoreWindow != null)
            {
                _highScoreWindow.Close();
            }

            _highScoreWindow = new HighScoreWindow();
            _highScoreWindow.Show();            
        }

        private void recordMI_Click(object sender, RoutedEventArgs e)
        {
            if (_recordWindow != null)
            {
                _recordWindow.Close();
            }

            _recordWindow = new RecordWindow();
            _recordWindow.FrameSource = _bitmap;
            _recordWindow.Show();
        }
    }
}
