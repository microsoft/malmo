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

namespace Microsoft.Research.Malmo.HumanAction
{
    /// <summary>
    /// Interaction logic for HighScoreWindow.xaml
    /// </summary>
    public partial class HighScoreWindow : Window
    {
        private TextBlock[] _scoreBlocks;
        private List<Score> _scores;

        public HighScoreWindow()
        {
            InitializeComponent();

            _scoreBlocks = new TextBlock[]{
                score1,
                score2,
                score3,
                score4,
                score5,
                score6,
                score7,
                score8,
                score9
            };

            _scores = Settings.Default.HighScores.Split(new char[] { ',' }, StringSplitOptions.RemoveEmptyEntries).Select(o => new Score(o)).ToList();

            Update();
        }

        public void AddScore(int score)
        {
            if (_scores.Count < _scoreBlocks.Length || score > _scores.Min(o=>o.Value))
            {
                NameDialog nameDialog = new NameDialog();
                if (nameDialog.ShowDialog() == true)
                {
                    string name = nameDialog.Result;
                    _scores.Add(new Score
                    {
                        Name = name,
                        Value = score
                    });
                }
            }

            Settings.Default.HighScores = string.Join(",", _scores.OrderByDescending(o=>o.Value).Take(_scoreBlocks.Length));
            Settings.Default.Save();

            Update();
        }

        public void Update()
        {
            var highScores = _scores.OrderByDescending(o => o.Value).Take(_scoreBlocks.Length).ToArray();

            Score defaultScore = new Score();
            for (int i = 0; i < _scoreBlocks.Length; i++ )
            {
                if (i < highScores.Length)
                {
                    _scoreBlocks[i].Text = highScores[i].ToScoreString(i);
                }
                else
                {
                    _scoreBlocks[i].Text = defaultScore.ToScoreString(i);
                }
            }
        }
    }
}
