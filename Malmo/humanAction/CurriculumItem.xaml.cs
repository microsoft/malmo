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
using System.Windows.Navigation;
using System.Windows.Shapes;

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for CurriculumItem.xaml
    /// </summary>
    public partial class CurriculumItem : UserControl
    {
        // do the right thing here with RoutedEvents
        public static RoutedEvent RemoveRequestedEvent = EventManager.RegisterRoutedEvent("RemoveRequested", RoutingStrategy.Bubble, typeof(RoutedEventHandler), typeof(CurriculumItem));
        public static RoutedEvent UpRequestedEvent = EventManager.RegisterRoutedEvent("UpRequested", RoutingStrategy.Bubble, typeof(RoutedEventHandler), typeof(CurriculumItem));
        public static RoutedEvent DownRequestedEvent = EventManager.RegisterRoutedEvent("DownRequested", RoutingStrategy.Bubble, typeof(RoutedEventHandler), typeof(CurriculumItem));

        // and then something like this?
        public event RoutedEventHandler RemoveRequested
        {
            add
            {
                base.AddHandler(CurriculumItem.RemoveRequestedEvent, value);
            }

            remove
            {
                base.RemoveHandler(CurriculumItem.RemoveRequestedEvent, value);
            }
        }

        public event RoutedEventHandler UpRequested
        {
            add
            {
                base.AddHandler(CurriculumItem.UpRequestedEvent, value);
            }

            remove
            {
                base.RemoveHandler(CurriculumItem.UpRequestedEvent, value);
            }
        }


        public event RoutedEventHandler DownRequested
        {
            add
            {
                base.AddHandler(CurriculumItem.DownRequestedEvent, value);
            }

            remove
            {
                base.RemoveHandler(CurriculumItem.DownRequestedEvent, value);
            }
        }


        public CurriculumItem(string filename)
        {
            InitializeComponent();

            this.Filename = filename;
            filenameTB.Text = System.IO.Path.GetFileNameWithoutExtension(filename);
        }

        public bool CanMoveUp
        {
            get
            {
                return upB.IsEnabled;
            }

            set
            {
                upB.IsEnabled = value;
            }
        }

        public bool CanMoveDown
        {
            get
            {
                return downB.IsEnabled;
            }

            set
            {
                downB.IsEnabled = value;
            }
        }

        public string Filename { get; set; }

        private void removeB_Click(object sender, RoutedEventArgs e)
        {
            base.RaiseEvent(new RoutedEventArgs(CurriculumItem.RemoveRequestedEvent, this));
        }

        private void upB_Click(object sender, RoutedEventArgs e)
        {
            base.RaiseEvent(new RoutedEventArgs(CurriculumItem.UpRequestedEvent, this));
        }

        private void downB_Click(object sender, RoutedEventArgs e)
        {
            base.RaiseEvent(new RoutedEventArgs(CurriculumItem.DownRequestedEvent, this));
        }
    }
}
