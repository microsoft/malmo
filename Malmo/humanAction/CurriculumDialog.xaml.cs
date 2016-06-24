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
