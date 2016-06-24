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
using System.IO;
using System.Linq;
using System.Net;
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

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for PreferencesDialog.xaml
    /// </summary>
    public partial class PreferencesDialog : Window
    {
        public PreferencesDialog()
        {
            InitializeComponent();

            string resolution = string.Format("{0} x {1}", Settings.Default.VideoWidth, Settings.Default.VideoHeight);
            bool found = false;
            foreach(var item in resolutionCB.Items)
            {
                ComboBoxItem cbi = item as ComboBoxItem;
                string text = cbi.Content as string;
                if (text.Equals(resolution))
                {
                    resolutionCB.SelectedItem = item;
                    found = true;
                    break;
                }
            }

            if (!found)
            {
                resolutionCB.SelectedIndex = 5;
                Settings.Default.VideoWidth = 320;
                Settings.Default.VideoHeight = 240;
            }

            recordObservationsCB.IsChecked = Settings.Default.RecordObservations;
            recordRewardsCB.IsChecked = Settings.Default.RecordRewards;
            recordCommandsCB.IsChecked = Settings.Default.RecordCommands;
            recordMP4CB.IsChecked = Settings.Default.RecordMP4;
            stickyKeysCB.IsChecked = Settings.Default.StickyKeys;
            bitrateSlider.Value = Math.Min(Math.Max(Settings.Default.BitRate, bitrateSlider.Minimum), bitrateSlider.Maximum);
            mp4FPSSlider.Value = Math.Min(Math.Max(Settings.Default.MP4FPS, mp4FPSSlider.Minimum), mp4FPSSlider.Maximum);
            moveCB.IsChecked = Settings.Default.IsMoveEnabled;
            strafeCB.IsChecked = Settings.Default.IsStrafeEnabled;
            pitchCB.IsChecked = Settings.Default.IsPitchEnabled;
            turnCB.IsChecked = Settings.Default.IsTurnEnabled;
            jumpCB.IsChecked = Settings.Default.IsJumpEnabled;
            crouchCB.IsChecked = Settings.Default.IsCrouchEnabled;
            attackCB.IsChecked = Settings.Default.IsAttackEnabled;
            useCB.IsChecked = Settings.Default.IsUseEnabled;
            outputDirTB.Text = Settings.Default.OutputDir;
            clientIPTB.Text = Settings.Default.ClientIP;
            clientPortTB.Text = Settings.Default.ClientPort.ToString();
        }

        private void bitrateSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            if (bitrateSlider.Value > 1000)
            {
                bitrateTB.Text = string.Format("{0:0.000} Mbps", bitrateSlider.Value * 0.001);
            }
            else
            {
                bitrateTB.Text = string.Format("{0} Kbps", (int)bitrateSlider.Value);
            }
        }

        private void recordMP4CB_Checked(object sender, RoutedEventArgs e)
        {
            if(bitrateSlider == null)
            {
                return;
            }

            bitrateSlider.IsEnabled = recordMP4CB.IsChecked == true;
            bitrateTB.IsEnabled = recordMP4CB.IsChecked == true;
            mp4FPSSlider.IsEnabled = recordMP4CB.IsChecked == true;
            mp4FPSTB.IsEnabled = recordMP4CB.IsChecked == true;
        }

        private void okB_Click(object sender, RoutedEventArgs e)
        {
            string outputDir = outputDirTB.Text.Trim();
            if (!string.IsNullOrEmpty(outputDir))
            {
                if (!Directory.Exists(outputDir))
                {
                    MessageBox.Show("Output folder does not exist.", "Missing Directory", MessageBoxButton.OK, MessageBoxImage.Warning);
                    return;
                }

                try
                {
                    string tempDir = System.IO.Path.Combine(outputDir, "test");
                    Directory.CreateDirectory(tempDir);

                    string path = System.IO.Path.Combine(tempDir, "test.txt");
                    using (StreamWriter test = File.CreateText(path))
                    {
                        test.WriteLine("test");
                    }

                    File.Delete(path);
                    Directory.Delete(tempDir);
                }
                catch (Exception ex)
                {
                    MessageBox.Show("Unable to use output folder: " + ex.Message, "I/O Error", MessageBoxButton.OK, MessageBoxImage.Error);
                    return;
                }
            }

            try
            {
                IPAddress.Parse(clientIPTB.Text);
            }
            catch
            {
                MessageBox.Show("Invalid IP Address", "Invalid Setting", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            try
            {
                short port = short.Parse(clientPortTB.Text);
                if (port < 10000 || port > 20000)
                {
                    throw new ArgumentOutOfRangeException("Port must be greater than 10000 and less than 20000");
                }
            }
            catch (ArgumentOutOfRangeException ex)
            {
                MessageBox.Show(ex.Message, "Invalid Setting", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }
            catch
            {
                MessageBox.Show("Invalid Port", "Invalid Setting", MessageBoxButton.OK, MessageBoxImage.Warning);
                return;
            }

            string[] parts = ((resolutionCB.SelectedItem as ComboBoxItem).Content as string).Split('x');
            Settings.Default.VideoWidth = short.Parse(parts[0].Trim());
            Settings.Default.VideoHeight = short.Parse(parts[1].Trim());

            Settings.Default.RecordObservations = recordObservationsCB.IsChecked == true;
            Settings.Default.RecordRewards = recordRewardsCB.IsChecked == true;
            Settings.Default.RecordCommands = recordCommandsCB.IsChecked == true;
            Settings.Default.RecordMP4 = recordMP4CB.IsChecked == true;
            Settings.Default.BitRate = bitrateSlider.Value;
            Settings.Default.MP4FPS = (int)mp4FPSSlider.Value;
            Settings.Default.StickyKeys = stickyKeysCB.IsChecked == true;
            Settings.Default.IsMoveEnabled = moveCB.IsChecked == true;
            Settings.Default.IsStrafeEnabled = strafeCB.IsChecked == true;
            Settings.Default.IsPitchEnabled = pitchCB.IsChecked == true;
            Settings.Default.IsTurnEnabled = turnCB.IsChecked == true;
            Settings.Default.IsJumpEnabled = jumpCB.IsChecked == true;
            Settings.Default.IsCrouchEnabled = crouchCB.IsChecked == true;
            Settings.Default.IsAttackEnabled = attackCB.IsChecked == true;
            Settings.Default.IsUseEnabled = useCB.IsChecked == true;
            Settings.Default.IsNorthEnabled = northCB.IsChecked == true;
            Settings.Default.IsSouthEnabled = southCB.IsChecked == true;
            Settings.Default.IsEastEnabled = eastCB.IsChecked == true;
            Settings.Default.IsWestEnabled = westCB.IsChecked == true;
            Settings.Default.OutputDir = outputDir;
            Settings.Default.ClientIP = clientIPTB.Text;
            Settings.Default.ClientPort = short.Parse(clientPortTB.Text);
            
            Settings.Default.Save();

            DialogResult = true;
        }

        private void cancelB_Click(object sender, RoutedEventArgs e)
        {
            DialogResult = false;
        }

        private void mp4FPSSlider_ValueChanged(object sender, RoutedPropertyChangedEventArgs<double> e)
        {
            mp4FPSTB.Text = string.Format("{0} fps", (int)mp4FPSSlider.Value);
        }

        private void clearScoresB_Click(object sender, RoutedEventArgs e)
        {
            Settings.Default.HighScores = "";
            Settings.Default.Save();            
        }
    }
}

