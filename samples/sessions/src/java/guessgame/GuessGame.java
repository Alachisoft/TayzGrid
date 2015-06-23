/*
* Copyright (c) 2015, Alachisoft. All Rights Reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package guessgame;

import java.util.Random;
import java.io.Serializable;
import java.util.ArrayList;

public class GuessGame implements Serializable
{
    private ArrayList<Integer> history = new ArrayList<Integer>();
    private int secretNumber = 0;
    private int maxUpperLimit = 101;
    private int min = 0;
    private int max = 101;

    //--------------------------------------------------------------------------
    public GuessGame()
    {
        initialize();
    }
    //--------------------------------------------------------------------------
    public void initialize()
    {
        Random rand = new Random();
        min = 0;
        max = maxUpperLimit;
        secretNumber = rand.nextInt(maxUpperLimit);
    }
    //--------------------------------------------------------------------------
    public boolean makeGuess(int guess)
    {
        history.add(guess);
        if (guess == secretNumber)
        {
            return true;
        }
        return false;
    }
    //--------------------------------------------------------------------------
    public void newGame()
    {
        history.clear();
        initialize();
    }
    //--------------------------------------------------------------------------
    public String getHistory()
    {
        String histText = "";
        for (int i = 0; i < history.size(); i++)
        {
            if (i > 0)
            {
                histText += ", ";
            }
            histText += history.get(i);
        }
        return histText;
    }
    //--------------------------------------------------------------------------
    public int getGuessCount()
    {
        return history.size();
    }
    //--------------------------------------------------------------------------
    public String getRetryInfo()
    {
        String message = "<p>";

        if (getGuessCount() == 0)
        {
            message = "You have not attempted yet.";
        }
        else
        {
            int num = getLastGuess();
            if (num < secretNumber)
            {
                message = "Attempt #" + getGuessCount() + " - The number that you have tried is SMALLER than the guess.</p>";
            }
            else if (num > secretNumber)
            {
                message = "Attempt #" + getGuessCount() + " - The number that you have tried is GREATER than the guess.</p>";
            }
        }
        message += "</p>";
        return message;
    }
    //--------------------------------------------------------------------------
    public String getHint()
    {
        String hint = "<p class=\"hint\">Hint: Number is between " + min + " and " + max + "</p>";
        if (getGuessCount() > 0)
        {
            int num = getLastGuess();
            if (num < secretNumber && num > min)
            {
                min = num;
            }
            if (num > secretNumber && num < max)
            {
                max = num;
            }

            if (num == secretNumber)
            {
                hint = "<p class=\"success\">Congratulations! You have found the Secret Number.</p>";
            }
            else
            {
                hint = "<p class=\"hint\">Hint: Number is between " + min + " and " + max + "</p>";
            }
        }
        return hint;
    }
    //--------------------------------------------------------------------------
    public int getLastGuess()
    {
        if (getGuessCount() == 0)
        {
            return -1;
        }
        else
        {
            return history.get(history.size() - 1);
        }
    }
    //--------------------------------------------------------------------------
}
