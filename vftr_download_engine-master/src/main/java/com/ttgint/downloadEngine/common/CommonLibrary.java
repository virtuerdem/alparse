/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

/**
 *
 * @author TTGParserTeam
 */
public class CommonLibrary {

    public static long getPid() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(processName.split("@")[0]);
    }

    public static class AnnatStaticOperations {

        public static boolean isDownloadEngine(Class classObject) {
            return classObject.isAnnotationPresent(DownloadEngine.class);
        }

        public static boolean isDownloadEngines(Class classObject) {
            return classObject.isAnnotationPresent(DownloadEngines.class);
        }

        public static DownloadEngine getDownlaodEngineAnat(Class classObject) {
            Annotation anat = classObject.getAnnotation(DownloadEngine.class);
            return (DownloadEngine) anat;
        }

        public static DownloadEngines getDownlaodEnginesAnat(Class classObject) {
            Annotation anat = classObject.getAnnotation(DownloadEngines.class);
            return (DownloadEngines) anat;
        }

    }

    public static String getGmt(Date date) {
        //Get gmt
        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyyMMddHHmm,Z");
        String sdfDate = sdf1.format(date);
        String gmtString = sdfDate.split(",")[1];
        return gmtString;
    }

    public static String get_CurrentDatetime(String dFormat) {
        DateFormat dateFormat = new SimpleDateFormat(dFormat);
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static String getPropertiesValues(String propertiesFileName, String key) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(new File(propertiesFileName)));
        String variable = prop.getProperty(key);

        return variable;
    }

    public static void deleteFolderAndContent(File directory) throws Exception {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteFolderAndContent(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        directory.delete();
    }

    public static ArrayList<File> list_AllFilesAsFile(String path) {
        ArrayList<File> allFiles = new ArrayList<>();
        Queue<File> dirs = new LinkedList<>();
        dirs.add(new File(path));
        while (!dirs.isEmpty()) {
            File[] listFile = dirs.poll().listFiles();
            if (listFile != null) {
                for (File f : listFile) {
                    if (f.isDirectory()) {
                        dirs.add(f);
                    } else if (f.isFile()) {
                        allFiles.add(f);
                    }
                }
            }
        }
        return allFiles;
    }

    public static void errorLogger(String err, int print) {
        if (DownloadApp.OPERATORNAME.equals("TURKTELEKOM")) {
            System.err.println(err);
            if (err.length() > 2501) {
                err = err.substring(0, 2500);
            }
            DaoUtils.executeQuery(DownloadQueries.insertErrorLog(DownloadApp.OPERATORNAME, DownloadApp.SYSTEMTYPE, DownloadApp.MEASTYPE, err.replace("'", ""), String.valueOf(0), String.valueOf(0)));
        } else if (print == 0) { //do nothing
        } else if (print == 1) { //sout
            System.out.println(err);
        } else if (print == 2) { //serr
            System.err.println(err);
        }

    }
}
