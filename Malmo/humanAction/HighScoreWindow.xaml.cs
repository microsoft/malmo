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
