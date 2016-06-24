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
