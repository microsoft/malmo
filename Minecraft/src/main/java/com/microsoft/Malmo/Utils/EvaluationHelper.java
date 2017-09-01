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

package com.microsoft.Malmo.Utils;

import java.util.Arrays;
import java.util.Random;
import java.util.Stack;

public class EvaluationHelper
{
    static public void test()
    {
        Random rand = new Random();
        String tests[] = {"2^(abs(t) % 3)", "200*(rand-0.5)", "-1--t", "sin(-t)", "t^2", "1.0/(abs(t)+1)"};
        for (int i = 0; i > -10; i--)
        {
            System.out.println("For t = " + i);
            for (int t = 0; t < tests.length; t++)
            {
                try
                {
                    System.out.println(tests[t] + " = " + eval(tests[t], i, rand));
                }
                catch (Exception e)
                {
                    System.out.println(e.getMessage());
                }
            }
        }
    }

    static final String[] functions = {"sin", "cos", "tan", "asin", "acos", "atan", "abs"};
    static final String[] tokens = {"t", "+", "-", "/", "*", "%", "^", "rand", "(", ")"};

    static public float eval(String expression, float t, Random rand) throws Exception
    {
        Stack<Float> values = new Stack<Float>();
        Stack<String> operators = new Stack<String>();
        boolean mustBeUnaryMinus = true;
        while (!expression.isEmpty())
        {
            String number = "";
            expression = expression.trim(); // Remove any white space
            // Is the next token a number?
            while (!expression.isEmpty() && expression.substring(0, 1).matches("[0-9\\.]"))
            {
                number += expression.substring(0,1);
                expression = expression.substring(1);
            }
            Float f;
            if (!number.isEmpty())
            {
                f = Float.valueOf(number);
                // Yes, so push it straight to the values stack:
                values.push(f);
                mustBeUnaryMinus = false;   // A unary '-' can't follow a value.
            }
            else
            {
                // Not a number - what is it?
                String op = null;
                for (int i = 0; i < functions.length + tokens.length; i++)
                {
                    String tok = (i < functions.length) ? functions[i] : tokens[i - functions.length];
                    if (expression.startsWith(tok))
                    {
                        // Found it.
                        op = tok;
                        expression = expression.substring(op.length());
                        break;
                    }
                }
                if (op == null) // unrecognised token
                    throw new Exception("Unrecognised token at start of " + expression + " in eval()");
                // Determine what to do for this token..
                if (op.equals("-") && mustBeUnaryMinus) // check for unary minus
                    op = "unary_minus"; // to distinguish from, for example, "a-b".
                if (op.equals("t"))
                {
                    // time variable - substitute actual value of t and push straight to values:
                    values.push(t);
                    mustBeUnaryMinus = false;   // A unary '-' can't follow a value.
                }
                else if (op.equals("rand"))
                {
                    // push a random value straight to values:
                    values.push(rand.nextFloat());
                    mustBeUnaryMinus = false;   // A unary '-' can't follow a value.
                }
                else if (op.equals("("))
                {
                    // push to operators stack
                    operators.push(op);
                    mustBeUnaryMinus = true;    // A '-' following a '(' must be a unary minus - eg "(-4*8)"
                }
                else if (op.equals(")"))
                {
                    // closing bracket - pop the operators until we find the matching opening bracket.
                    while (!operators.isEmpty())
                    {
                        op = operators.pop();
                        if (op.equals("("))
                            break;
                        doOp(op, values, operators, rand);  // carry out this operation.
                    }
                    mustBeUnaryMinus = false;   // A '-' following a ')' can't be a unary minus - eg "(4^2)-16"
                }
                else if (isFunction(op))    // sin, cos, abs, etc. go straight on the operator stack
                    operators.push(op);
                else
                {
                    // token is an operator - need to consider the operator precedence, and pop any stacked operators
                    // that should be applied first.
                    int precedence = getPrecedence(op);
                    if (isRightAssociative(op))
                        precedence++;   // Force <= comparison to behave as < comparison.
                    while (!operators.isEmpty() && !operators.peek().equals("(") && precedence <= getPrecedence(operators.peek()))
                    {
                        String op2 = operators.pop();
                        doOp(op2, values, operators, rand);
                    }
                    operators.push(op);
                    mustBeUnaryMinus = true;    // A '-' following another operator must be unary - eg "4*-7"
                }
            }
        }
        // Finished going through the input string - apply any outstanding operators, functions, etc:
        while (!operators.empty())
        {
            String op = operators.pop();
            doOp(op, values, operators, rand);
        }
        return values.pop();
    }

    private static boolean isFunction(String op)
    {
        return Arrays.asList(functions).contains(op);
    }

    private static int getPrecedence(String op)
    {
        if (op.equals("+") || op.equals("-"))
            return 2;
        else if (op.equals("*") || op.equals("/"))
            return 3;
        else if (op.equals("^"))
            return 4;
        else if (op.equals("unary_minus"))
            return 5;
        return 6;
    }

    private static boolean isRightAssociative(String op)
    {
        if (op.equals("unary_minus") || op.equals("^"))
            return true;
        return false;
    }

    private static void doOp(String op, Stack<Float> values, Stack<String> operators, Random rand)
    {
        if (op.equals("+"))
            values.push(values.pop() + values.pop());
        else if (op.equals("-"))
            values.push(-(values.pop() - values.pop()));
        else if (op.equals("*"))
            values.push(values.pop() *  values.pop());
        else if (op.equals("/"))
        {
            Float b = values.pop();
            Float a = values.pop();
            values.push(a / b);
        }
        else if (op.equals("%"))
        {
            int b = Math.round(values.pop());
            int a = Math.round(values.pop());
            values.push((float)(a % b));
        }
        else if (op.equals("^"))
        {
            Float b = values.pop();
            Float a = values.pop();
            values.push((float)Math.pow(a, b));
        }
        else if (op.equals("sin"))
        {
            values.push((float)(Math.sin(values.pop())));
        }
        else if (op.equals("cos"))
        {
            values.push((float)(Math.cos(values.pop())));
        }
        else if (op.equals("tan"))
        {
            values.push((float)(Math.tan(values.pop())));
        }
        else if (op.equals("asin"))
        {
            values.push((float)(Math.asin(values.pop())));
        }
        else if (op.equals("acos"))
        {
            values.push((float)(Math.acos(values.pop())));
        }
        else if (op.equals("atan"))
        {
            values.push((float)(Math.atan(values.pop())));
        }
        else if (op.equals("abs"))
        {
            values.push(Math.abs(values.pop()));
        }
        else if (op.equals("unary_minus"))
        {
            values.push(-values.pop());
        }
    }
}
