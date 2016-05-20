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
using Microsoft.Research.Malmo.HumanAction.Properties;
using System.IO;

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for CurriculumDialog.xaml
    /// </summary>
    public partial class CurriculumDialog : Window
    {   
        public CurriculumDialog()
        {
            InitializeComponent();

            string[] paths = Settings.Default.Curriculum.Split(new char[] { '|' }, StringSplitOptions.RemoveEmptyEntries);
            foreach(string path in paths)
            {
                this.addItem(path);
            }

            this.updateButtons();
        }

        public IEnumerable<string> Items
        {
            get
            {
                foreach (var item in missionSP.Children)
                {
                    yield return (item as CurriculumItem).Filename;
                }
            }
        }

        private async void addB_Click(object sender, RoutedEventArgs e)
        {
            var ofd = new Win32.OpenFileDialog();
            ofd.Filter = "Mission Files (*.xml)|*.xml";

            if (ofd.ShowDialog(this) == true)
            {
                using (StreamReader reader = new StreamReader(ofd.FileName))
                {
                    try
                    {
                        MissionSpec mission = new MissionSpec(await reader.ReadToEndAsync(), true);
                    }
                    catch (Exception ex)
                    {
                        MessageBox.Show(ex.Message, "Error Parsing Mission", MessageBoxButton.OK, MessageBoxImage.Error);
                        return;
                    }
                }

                this.addItem(ofd.FileName);

                this.updateButtons();
            }
        }

        private void addItem(string filename)
        {
            CurriculumItem item = new CurriculumItem(filename);
            item.Margin = new Thickness(0, 5, 0, 0);
            item.RemoveRequested += Item_RemoveRequested;
            item.UpRequested += Item_UpRequested;
            item.DownRequested += Item_DownRequested;
            missionSP.Children.Add(item);
        }

        private void Item_DownRequested(object sender, RoutedEventArgs e)
        {
            CurriculumItem item = e.Source as CurriculumItem;
            int index = missionSP.Children.IndexOf(item) + 1;
            missionSP.Children.Remove(item);
            missionSP.Children.Insert(index, item);

            this.updateButtons();
        }

        private void Item_UpRequested(object sender, RoutedEventArgs e)
        {
            CurriculumItem item = e.Source as CurriculumItem;
            int index = missionSP.Children.IndexOf(item) - 1;
            missionSP.Children.Remove(item);
            missionSP.Children.Insert(index, item);

            this.updateButtons();
        }

        private void updateButtons()
        {
            int count = missionSP.Children.Count;
            for (int i = 0; i < count; i++)
            {
                CurriculumItem item = missionSP.Children[i] as CurriculumItem;
                item.CanMoveUp = i > 0;
                item.CanMoveDown = i < count - 1;
            }
        }

        private void Item_RemoveRequested(object sender, RoutedEventArgs e)
        {
            missionSP.Children.Remove(e.Source as CurriculumItem);
        }

        private void okB_Click(object sender, RoutedEventArgs e)
        {
            Settings.Default.Curriculum = string.Join("|", this.Items);
            Settings.Default.Save();

            DialogResult = true;
        }

        private void cancelB_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
        }
    }
}
