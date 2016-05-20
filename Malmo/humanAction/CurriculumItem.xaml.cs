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
