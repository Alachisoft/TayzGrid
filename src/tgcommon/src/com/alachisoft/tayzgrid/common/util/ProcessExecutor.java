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
package com.alachisoft.tayzgrid.common.util;

import com.alachisoft.tayzgrid.common.Common;
import com.alachisoft.tayzgrid.runtime.util.RuntimeUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Map;

public class ProcessExecutor {

    private ArrayList<String> command = new ArrayList<String>();
    private BufferedReader outputStream;
    private BufferedReader errorStream;
    private OutputStreamWriter inputStream;
    private final long maxInterval = 10000;

    public ProcessExecutor(ArrayList<String> command) {
        this.command = command;
    }

    public Process execute() throws Exception {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            Map<String, String> mapSubProcess = builder.environment();
            mapSubProcess.putAll(System.getenv());
            process = builder.start();
            //inputStream = new OutputStreamWriter(process.getOutputStream());
            
            outputStream = new BufferedReader(new InputStreamReader(process.getInputStream()));
            errorStream = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        } catch (IOException exp) {
            kill(process);
            throw exp;
        }
        return process;
    }

    public void kill(Process process) {
        if (process != null) {
            try {
                close(process);
                process.destroy();
            } catch (Exception ex) {
            }
        }
    }

    public void close(Process process) throws Exception {

        if (process != null) {
            if (process.getErrorStream() != null) {
                process.getErrorStream().close();
            }
            if (process.getInputStream() != null) {
                process.getInputStream().close();
            }
            if (process.getOutputStream() != null) {
                process.getOutputStream().close();
            }
            process = null;
        }

        if (errorStream != null) {
            errorStream.close();
        }

        if (outputStream != null) {
            outputStream.close();
        }

    }

    public void readOutput(Process process) throws Exception {
        try {
            StringBuilder errorBuilder = new StringBuilder();
            boolean errorOccur = false;
            long t = System.currentTimeMillis();
            long end = t + maxInterval;
            do {
                String line;
                
                if (outputStream != null) {
                    if (outputStream.ready()) {
                        if ((line = outputStream.readLine()) != null) {
                            if (!Common.isNullorEmpty(line) && line.contains("Started")) {
                                close(process);
                                break;
                            }
                        }
                    }
                }
                if (errorStream != null) {
                    if (errorStream.ready()) {
                        if ((line = errorStream.readLine()) != null) {
                            if (!Common.isNullorEmpty(line)) {                                
                                if(!line.contains("VM warning: ignoring option UseSplitVerifier;")){
                                errorBuilder.append(line);
                                errorOccur = true;
                                }
                            }
                        }
                    }
                }
                Thread.sleep(300);

            }while (System.currentTimeMillis() < end);
            
            if(errorOccur)
            {
                kill(process);
                throw new Exception(errorBuilder.toString());
            }
        } catch (Exception exp) {
            throw exp;
        }
    }

    private static boolean isProcessRunning(int pid) {

        RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
        ArrayList<String> cArrayList = new ArrayList<String>();
        try {
            String cmd="";
            if (currentOS == RuntimeUtil.OS.Linux) {
                cArrayList.add("ps aux | grep " + pid + "");
            } else if (currentOS == RuntimeUtil.OS.Windows) {
                cArrayList.add("cmd");
                cArrayList.add("/c");
                cArrayList.add("tasklist /FI \"PID eq " + pid + "\"");
            }
            for (int i = 0; i < cArrayList.size(); i++) {
                cmd += cArrayList.get(i)+" ";
            }
            ProcessBuilder runtime = new ProcessBuilder(cArrayList);
            Process proc = runtime.start();
            InputStream inputstream = proc.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                if (line.contains(" " + pid + " ")) {
                    return true;
                }
            }
        } catch (IOException ex) {
        }
        return false;
    }

    public static void killProcess(int pid) {
        if (isProcessRunning(pid)) 
        {
            RuntimeUtil.OS currentOS = RuntimeUtil.getCurrentOS();
            ArrayList<String> cArrayList = new ArrayList<String>();
            try {
                if (currentOS == RuntimeUtil.OS.Linux) {
                    cArrayList.add("kill -9 " + pid + "");
                } else if (currentOS == RuntimeUtil.OS.Windows) {
                    cArrayList.add("cmd");
                    cArrayList.add("/c");
                    cArrayList.add("taskkill /PID ");   //this space after /PID is very important
                    cArrayList.add(pid + " /F");        //do not give space before pid its very important
                }
                
                ProcessBuilder runtime = new ProcessBuilder(cArrayList);
                Process proc = runtime.start();
                
                StringBuilder errorBuilder = new StringBuilder();
                boolean errorOccur = false;
                BufferedReader errorStream = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

                long t = System.currentTimeMillis();
                long end = t + 3000;
                do {
                    String line;
                    if (errorStream != null) {
                        if (errorStream.ready()) {
                            if ((line = errorStream.readLine()) != null) {
                                if (!Common.isNullorEmpty(line)) {
                                    errorBuilder.append(line);
                                    errorOccur = true;
                                }
                            } 
                        }
                    }
                    Thread.sleep(300);
                } 
                while (System.currentTimeMillis() < end);
                if (errorOccur) 
                {
                    runtime = new ProcessBuilder(cArrayList);
                    proc = runtime.start();
                }
            }
            catch (Exception ex){}
        }
    }
}
