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

package com.alachisoft.tayzgrid.utility.jarscanner;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class JARReflector {

    HashMap <String,String>fieldsType; 

    public JARReflector() {
        fieldsType = new HashMap<String,String>();
        fieldsType.put("int", "java.lang.Integer");
        fieldsType.put("short", "java.lang.Integer");
        fieldsType.put("long", "java.lang.Long");
        fieldsType.put("float", "java.lang.Float");
        fieldsType.put("byte", "java.lang.Byte");
        fieldsType.put("double", "java.lang.Double");
        fieldsType.put("char", "java.lang.Character");
        fieldsType.put("boolean", "java.lang.Boolean");
    }
    
    
    /**
     *
     * @param path Representing the physical path of .jar or .class file
     * @param interfaceName Name of interface
     * @return Array of String containing class Names that implements
     * interfaceName
     * @throws Exception Throws Exception if this method is unable to load the
     * file
     *
     */
    public String[] getClassNames(String path, String interfaceName) throws Exception {
        try {
            List list = getClasses(path, interfaceName);
            if (list == null && list.size() <= 0) {
                return null;
            }
            Collections.sort(list);
            String[] classes = (String[]) list.toArray(new String[0]);
            return classes;
        } catch (Exception ex) {
            throw ex;
        }
    }

    private List getClasses(String path, String interfaceName) throws Exception {
        List list = new ArrayList();

        if (path.endsWith(".jar")) {

            List l = JarUtils.getClasseNamesInJAR(path);
            if (l != null && l.size() <= 0) {
                return null;
            }
            try {
                URL urls[]
                        = {
                            new File(path).toURI().toURL()
                        };

                JarFileLoader cl = new JarFileLoader(urls);
                Class interFace = Class.forName(interfaceName);

                for (int i = 0; i < l.size(); i++) {

                    String[] tempClass = l.get(i).toString().split(".class");
                    Class cls = cl.loadClass(tempClass[0]);
                    boolean match = isValidClass(cls, interFace);
                    if (match) {
                        list.add(tempClass[0]);
                    }
                }

                Collections.sort(list);
                return list;
            } catch (Exception ex) {
                throw ex;
            }
        } else if (path.endsWith(".class")) {
            try {
                File tempFile = new File(path);
                String pth = path.replaceAll(tempFile.getName(), "");
                URL urls[]
                        = {
                            new File(path).toURI().toURL()
                        };
                Class interFace = Class.forName(interfaceName);
                String[] tempClass = (tempFile.getName()).split("\\.");
                URLClassLoader clLoader = new URLClassLoader(urls);
                Class cls = clLoader.loadClass(tempClass[0]);
                boolean match = isValidClass(cls, interFace);
                if (match) {
                    list.add(tempClass[0]);
                }
            } catch (Exception e) {
                throw e;
            }
        }
        return list;
    }

    private boolean isValidClass(Class cls, Class interFace) {
        boolean match = !cls.isInterface() && !cls.isEnum() && interFace.isAssignableFrom(cls);
        return match;
    }

    private boolean isClass(Class cls) {
        boolean match = !cls.isInterface() && !cls.isEnum();
        return match;
    }

    /**
     *
     * @param path Representing the physical path of .jar or .class file
     * @return Array of String containing class Names that implements
     * interfaceName
     * @throws Exception Throws Exception if this method is unable to load the
     * file
     *
     */
    public HashMap<String, List<Fields>> getFields(String path) throws Exception {
        HashMap<String, List<Fields>> map = new HashMap<String, List<Fields>>();
        try {
            if (path.endsWith(".jar")) {

                List l = JarUtils.getClasseNamesInJAR(path);
                if (l != null && l.size() <= 0) {
                    return null;
                }
                URL urls[]
                        = {
                            new File(path).toURI().toURL()
                        };

                JarFileLoader cl = new JarFileLoader(urls);
                for (int i = 0; i < l.size(); i++) {
                    String[] tempClass = l.get(i).toString().split(".class");
                    Class cls = cl.loadClass(tempClass[0]);
                    if (isClass(cls)) {
                        List<Fields> fieldList = getFields(cls);
                        map.put(cls.getName().toString(), fieldList);
                    }
                }
            } else if (path.endsWith(".class")) {
                File tempFile = new File(path);
                String pth = path.replaceAll(tempFile.getName(), "");
                File file = new File(pth);
                URL url = file.toURL();
                URL[] urls = new URL[]{
                    url
                };
                String[] tempClass = (tempFile.getName()).split("\\.");
                URLClassLoader clLoader = new URLClassLoader(urls);
                Class cls = clLoader.loadClass(tempClass[0]);
                if (isClass(cls)) {
                    List<Fields> fieldList = getFields(cls);
                    if (fieldList != null) {
                        map.put(cls.getName().toString(), fieldList);
                    }
                }
            }

            return map;

        } catch (Exception e) {
            throw e;
        }
        // return null;
    }

    private List<Fields> getFields(Class cls) {
        List<Fields> fieldList = new ArrayList<Fields>();
        Field[] fields = cls.getFields();
        //Field[] fields = cls.getDeclaredFields();
        
        for (int i = 0; i < fields.length; i++) {
            fieldList.add(new Fields(fields[i].getType().getCanonicalName().toString(), fields[i].getName().toString()));
        }
        return fieldList;
    }
    
    
        /**
     *
     * @param path Representing the physical path of .jar or .class file
     * @return Array of String containing class Names that implements
     * interfaceName
     * @throws Exception Throws Exception if this method is unable to load the
     * file
     *
     */
    public HashMap<String, List<Fields>> getFieldsFullyQualify(String path) throws Exception {
        HashMap<String, List<Fields>> map = new HashMap<String, List<Fields>>();
        try {
            if (path.endsWith(".jar")) {

                List l = JarUtils.getClasseNamesInJAR(path);
                if (l != null && l.size() <= 0) {
                    return null;
                }
                URL urls[]
                        = {
                            new File(path).toURI().toURL()
                        };

                JarFileLoader cl = new JarFileLoader(urls);
                //cl.addFile(path);
                for (int i = 0; i < l.size(); i++) {
                    String[] tempClass = l.get(i).toString().split(".class");
                    Class cls = cl.loadClass(tempClass[0]);
                    if (isClass(cls)) {
                        List<Fields> fieldList = getFieldsFullyQualify(cls);
                        map.put(cls.getName().toString(), fieldList);
                    }
                }
            } else if (path.endsWith(".class")) {
                File tempFile = new File(path);
                String pth = path.replaceAll(tempFile.getName(), "");
                File file = new File(pth);
                URL url = file.toURL();
                URL[] urls = new URL[]{
                    url
                };
                String[] tempClass = (tempFile.getName()).split("\\.");
                URLClassLoader clLoader = new URLClassLoader(urls);
                Class cls = clLoader.loadClass(tempClass[0]);
                if (isClass(cls)) {
                    List<Fields> fieldList = getFieldsFullyQualify(cls);
                    if (fieldList != null) {
                        map.put(cls.getName().toString(), fieldList);
                    }
                }
            }

            return map;

        } catch (Exception e) {
            throw e;
        }
    }
    
   
    
     private List<Fields> getFieldsFullyQualify(Class cls) {
        List<Fields> fieldList = new ArrayList<Fields>();
        Field[] fields = cls.getFields();

        for (int i = 0; i < fields.length; i++) {
            if(fields[i].getType().isPrimitive()){
               fieldList.add(new Fields(fieldsType.get(fields[i].getType().getCanonicalName().toString()), fields[i].getName().toString()));
            }
            else{
                fieldList.add(new Fields(fields[i].getType().getCanonicalName().toString(),fields[i].getName().toString()));
            }
        }
        return fieldList;
    }

    public HashMap<String, String> getManifestAttributes(String path) throws Exception {

        JarFile jarfile = new JarFile(path);

        Manifest manifest = jarfile.getManifest();

        Attributes mattr = manifest.getMainAttributes();

        HashMap<String, String> mainAttributes = new HashMap<String, String>();
        for (Object a : mattr.keySet()) {
            mainAttributes.put(a.toString(), mattr.getValue((Name) a));
        }
        return mainAttributes;

    }

    /**
     *
     * @param path Representing the physical path of .jar or .class file
     * @return Array of String containing class Names that implements
     * interfaceName
     * @throws Exception Throws Exception if this method is unable to load the
     * file
     *
     */
    public String[] getClasses(String path) {
        List list = new ArrayList();
        try {
            if (path.endsWith(".jar")) {

                List l = JarUtils.getClasseNamesInJAR(path);
                if (l != null && l.size() <= 0) {
                    return null;
                }

                URL urls[]
                        = {
                            new File(path).toURI().toURL()
                        };
                JarFileLoader cl = new JarFileLoader(urls);
                for (int i = 0; i < l.size(); i++) {
                    String[] tempClass = l.get(i).toString().split(".class");
                    Class cls = cl.loadClass(tempClass[0]);
                    if (isClass(cls)) {
                        list.add(cls.getName().toString());
                    }
                }
            }

            if (list == null && list.size() <= 0) {
                return null;
            }

            Collections.sort(list);
            String[] classes = (String[]) list.toArray(new String[0]);
            return classes;

        } catch (Exception e) {
        }
        return null;
    }

    public java.lang.Class getClassForName(String path, String ClassName) {
        try {

            URL urls[]
                    = {
                        new File(path).toURI().toURL()
                    };
            JarFileLoader cl = new JarFileLoader(urls);
            Class resultClass = Class.forName(ClassName);
            return resultClass;

        } catch (Exception ex) {
            return null;
        }
    }
}
