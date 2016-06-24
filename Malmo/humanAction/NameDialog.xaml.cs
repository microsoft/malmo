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
using System.Diagnostics;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Data;
using System.Windows.Documents;
using System.Windows.Input;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using System.Windows.Shapes;
using System.Windows.Threading;

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for NameDialog.xaml
    /// </summary>
    public partial class NameDialog : Window
    {
        private DispatcherTimer timer;

        private TextBlock[] letters;
        private Canvas[] boxes;
        private int index;
        

        public NameDialog()
        {
            InitializeComponent();
        }

        public string Result
        {
            get
            {
                return string.Join("", this.letters.Select(o => o.Text));
            }
        }

        private async void Timer_Tick(object sender, EventArgs e)
        {
            if(this.index >= this.letters.Length)
            {
                return;
            }

            TextBlock currentLetter = this.letters[this.index];
            Canvas currentBox = this.boxes[this.index];

            Brush fg = currentLetter.Foreground;
            Brush bg = currentBox.Background;

            currentBox.Background = fg;
            currentLetter.Foreground = bg;

            await Task.Delay(200);

            currentBox.Background = bg;
            currentLetter.Foreground = fg;
        }

        private void Window_Loaded(object sender, RoutedEventArgs e)
        {
            timer = new DispatcherTimer();
            timer.Interval = TimeSpan.FromMilliseconds(500);
            timer.Tick += Timer_Tick;
            timer.Start();

            this.letters = new TextBlock[] { letter0TB, letter1TB, letter2TB, letter3TB };
            this.boxes = new Canvas[] { box0C, box1C, box2C, box3C };
        }

        private void Window_KeyDown(object sender, KeyEventArgs e)
        {
            char letter;
            if(e.Key >= Key.NumPad0 && e.Key <= Key.NumPad9)
            {
                letter = (char)((e.Key - Key.NumPad0) + '0');
            }
            else if(e.Key >= Key.A && e.Key <= Key.Z)
            {
                letter = (char)((e.Key - Key.A) + 'A');
            }
            else
            {
                return;
            }

            this.letters[this.index].Text = letter.ToString();
            this.index++;

            if(this.index == 4)
            {
                DialogResult = true;
                this.timer.Stop();
            }
        }
    }
}
