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

package com.alachisoft.tayzgrid.tools;

import com.alachisoft.tayzgrid.tools.common.CommandLineArgumentParser;
import tangible.RefObject;

public final class stresstesttool {

    public static StressTestToolParam cParam = new StressTestToolParam();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try 
        {
            stresstesttool.Run(args);
        } 
        catch (Exception e) 
        {
            System.err.println(e);
        }
        finally
        {
            System.exit(0);
        }
    }

    static public void Run(String[] args) {
        try {
            Object param = new StressTestToolParam();
            tangible.RefObject<Object> tempRef_param = new RefObject<Object>(param);
            CommandLineArgumentParser.CommandLineParser(tempRef_param, args);
            param = tempRef_param.argvalue;
            cParam = (StressTestToolParam) param;


            if (cParam.getIsUsage()) {
                AssemblyUsage.PrintUsage();
                return;
            }

            if (!ValidateParameters()) {
                return;
            }


            System.out.println("cacheId = " + cParam.getCacheId() + ", total-loop-count = " + cParam.getTotalLoopCount() + ", test-case-iterations = " + cParam.getTestCaseIterations() + ", testCaseIterationDelay = " + cParam.getTestCaseIterationDelay() + ", gets-per-iteration = " + cParam.getGetsPerIteration() + ", updates-per-iteration = " + cParam.getUpdatesPerIteration() + ", data-size = " + cParam.getDataSize() + ", expiration = " + cParam.getExpiration() + ", thread-count = " + cParam.getThreadCount() + ", reporting-interval = " + cParam.getReportingInterval() + ".");
            System.out.println("-------------------------------------------------------------------\n");

            ThreadTest threadTest = new ThreadTest(cParam.getCacheId(), cParam.getTotalLoopCount(), cParam.getTestCaseIterations(), cParam.getTestCaseIterationDelay(), cParam.getGetsPerIteration(), cParam.getUpdatesPerIteration(), cParam.getDataSize(), cParam.getExpiration(), cParam.getThreadCount(), cParam.getReportingInterval(), cParam.getIsLogo());
            threadTest.Test();
        } catch (Exception e) {
            System.err.println("Error :- " + e.getMessage());
        }
    }

    private static boolean ValidateParameters() {
        // Validating CacheId
        
        AssemblyUsage.PrintLogo(cParam.getIsLogo());

        if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(cParam.getCacheId())) {
            System.err.println("Error: Cache Name not specified");
            return false;
        }

        return true;
    }
}
