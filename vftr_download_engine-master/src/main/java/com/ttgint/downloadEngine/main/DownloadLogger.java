/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.main;

/**
 *
 * @author TTGETERZI
 */
public class DownloadLogger {

    public synchronized static void printLogForManager(String message, ManagerLogLevel level) {
        if (DownloadProperties.getInstance().isLogForManagerActive()) {
            String outMessage = "$ttg;" + level.getLevenName() + "=" + message;
            System.out.println(outMessage);
        }
    }

    public enum ManagerLogLevel {

        PID("Pid"), STATE("State");

        String levelName;

        ManagerLogLevel(String name) {
            this.levelName = name;
        }

        String getLevenName() {
            return levelName;
        }

    }

}
