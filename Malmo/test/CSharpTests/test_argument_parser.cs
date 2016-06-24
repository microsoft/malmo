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
using Microsoft.Research.Malmo;

class Program
{
    public static void Main(string[] args)
    {
        using (ArgumentParser parser = new ArgumentParser("test_argument_parser.cs"))
        {
            parser.addOptionalIntArgument("runs", "how many runs to perform", 1);
            parser.addOptionalFlag("verbose", "print lots of debugging information");
            parser.addOptionalFloatArgument("q", "what is the q parameter", 0.01);
            parser.addOptionalStringArgument("mission", "the mission filename", "");

            string[] args1 = new string[] { "filename", "--runs", "3", "--q", "0.2", "--mission", "test.xml", "--verbose" };
            using (StringVector sargs1 = new StringVector(args1))
            {
                try
                {
                    parser.parse(sargs1);
                }
                catch
                {
                    Environment.Exit(1);
                }
            }

            if (!parser.receivedArgument("runs") || parser.getIntArgument("runs") != 3)
                Environment.Exit(1);

            if (!parser.receivedArgument("q") || Math.Abs(0.2 - parser.getFloatArgument("q")) > 1E-06)
                Environment.Exit(1);

            if (!parser.receivedArgument("mission") || parser.getStringArgument("mission") != "test.xml")
                Environment.Exit(1);

            if (!parser.receivedArgument("verbose"))
                Environment.Exit(1);

            if (parser.receivedArgument("test.xml"))
                Environment.Exit(1);

            // we expect this to give an error
            string[] args2 = new string[]{ "filename", "--runs" };
            using (StringVector sargs2 = new StringVector(args2))
            {
                try
                {
                    parser.parse(sargs2);
                }
                catch
                {
                    // this is what we expect to happen
                    Environment.Exit(0);
                }
            }

            Environment.Exit(1);
        }
    }
}
