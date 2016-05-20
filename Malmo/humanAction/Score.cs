using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Microsoft.Research.Malmo.HumanAction
{
    public class Score
    {
        public const int NameLength = 4;
        public const int ValueLength = 5;

        public Score(string scoreText)
        {
            string[] parts = scoreText.Split(new char[] { ':' }, StringSplitOptions.RemoveEmptyEntries);
            Name = parts[0];
            Value = Convert.ToInt32(parts[1]);
        }

        public Score()
        {
            Name = "".PadLeft(NameLength, 'A');
        }

        public string Name { get; set; }
        public int Value { get; set; }

        public override string ToString()
        {
            return string.Format("{0}:{1}", Name.Trim(), Value);
        }

        public string ToScoreString(int order)
        {
            return string.Format(
                "{0}. {1} {2}", 
                order + 1, 
                this.Name.ToUpper().Substring(0, NameLength).PadRight(NameLength, ' '),
                this.Value.ToString().PadLeft(Score.ValueLength, '0'));
        }
    }
}
