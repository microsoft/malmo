// --------------------------------------------------------------------------------------------------------------------
// Copyright (C) Microsoft Corporation.  All rights reserved.
// --------------------------------------------------------------------------------------------------------------------

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
