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
