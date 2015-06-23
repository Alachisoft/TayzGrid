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
package com.alachisoft.tayzgrid.common;



import java.io.File;

public class Common {

    private final static Boolean debug = false;

    /**
     *
     * java alterative to c# "as"
     *
     * @param obj
     * @param cls
     * @return
     */
    public static Object readAs(Object obj, java.lang.Class cls) {
        return (cls.isInstance(obj) ? obj : null);
    }

    /**
     *
     * java alternative to C# "as" removes the hassle of casting each time at
     * users end in contrast to readAs(Object obj, java.lang.Class cls)
     *
     * @param <K>
     * @param <V>
     * @param obj
     * @param type
     * @return
     */
    public static <K, V> V as(K obj, Class<V> type) {
        return type.isInstance(obj) ? type.cast(obj) : null;
    }

    public static boolean is(Object obj, java.lang.Class cls) {
        return cls.isInstance(obj);
    }

    /**
     * Rounds off fraction to the given amount of precision. Faster than
     * DecimalFormat
     *
     * @param fraction
     * @param precision
     * @return
     */
    public static double roundOff(double fraction, int precision) {
        int i = 0;
        int finalNum = 1;
        while (i++ < precision) {
            finalNum *= 10;
        }
        return (double) Math.round(fraction * finalNum) / finalNum;
    }

    /**
     *
     * [Combine MultiplePaths] java alternative (OS independent) to
     * System.IO.Path.Combine().
     *
     * @param parentDir
     * @param childDirs
     * @return
     */
    public static String combinePath(String parentDir, String... childDirs) {
        return combinePath(new java.io.File(parentDir), childDirs);
    }

    /**
     *
     * [Combine MultiplePaths] java alternative (OS independent) to
     * System.IO.Path.Combine().
     *
     * @param parentDir
     * @param childDirs
     * @return
     */
    public static String combinePath(java.io.File parentDir, String... childDirs) {
        java.io.File path = parentDir;

        for (String childDir : childDirs) {
            path = new java.io.File(path, childDir);
        }
        return path.getPath();
    }

    /**
     *
     * Generates a random number in the provided range.
     *
     * @param max
     * @param min
     * @return
     */
    public static int generateRandomNoInRange(int max, int min) {
        return min + (int) Math.random() * ((max - min) + 1);
    }

    /**
     * Equivalent to .Net String.isNullorEmpty()
     *
     * @param value String value to compare
     * @return true if string is either null or length == 0 , otherwise false
     */
    public static boolean isNullorEmpty(String value) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return false;
    }

    public static String getTGHome() {

        String tg_home = System.getenv("TG_HOME");
        if (tg_home == null) {
            throw new RuntimeException("Unable to read the environment variable TG_HOME.");
        }
        return tg_home;
    }

    public static String getConfigPath() {
        File configPath = null;
        String path = "";
        String[] paths = new String[]{
            getTGHome(),
            ServicePropValues.CACHE_USER_DIR,
           
            "../"
        };
        for (String pathValue : paths) {
            if (pathValue != null) {
                configPath = new File(pathValue);
                if (configPath.exists()) {
                    path = configPath.getAbsolutePath();
                    break;
                }
            }

        }
        if (path.isEmpty()) {
            path = "./";
        }
        return path;
    }

    public static boolean isFileExist(String path) {

        if (isNullorEmpty(path)) {
            return false;
        }
        File file = new File(path);
        if (file.isDirectory() || !file.exists()) {
            return false;
        } else {
            return true;
        }

    }

}
