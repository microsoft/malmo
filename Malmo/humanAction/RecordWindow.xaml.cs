using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for RecordWindow.xaml
    /// </summary>
    public partial class RecordWindow : Window
    {
        private const int MAX_LENGTH = 20 * 1024;

        public RecordWindow()
        {
            InitializeComponent();
        }

        public BitmapSource FrameSource
        {
            set
            {
                frameI.Source = value;
            }
        }

        public string RecordPath
        {
            set
            {
                recordTB.Text = value;
            }
        }

        public string MissionPath
        {
            set
            {
                missionTB.Text = value;
            }
        }

        public TimeSpan Elapsed
        {
            set
            {
                string hours = value.Hours.ToString().PadLeft(2, '0');
                string minutes = value.Minutes.ToString().PadLeft(2, '0');
                string seconds = value.Seconds.ToString().PadLeft(2, '0');
                string milliseconds = value.Milliseconds.ToString().PadLeft(3, '0');
                elapsedTB.Text = string.Format("{0}:{1}:{2}.{3}", hours, minutes, seconds, milliseconds);
            }
        }

        public void Write(IEnumerable<string> messages)
        {
            if(!messages.Any()){
                return;
            }

            string newText = consoleTB.Text + Environment.NewLine + string.Join(Environment.NewLine, messages);

            if (newText.Length > MAX_LENGTH)
            {
                newText = newText.Substring(newText.Length - MAX_LENGTH);
            }

            consoleTB.Text = newText;

            consoleSV.ScrollToBottom();
        }

        public void Clear()
        {
            consoleTB.Text = "";
        }
    }
}
