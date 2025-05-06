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
public class DownloadProperties {

    private static DownloadProperties instance;

    private boolean logforManagerflag = false;

    private DownloadProperties() {
    }

    public static DownloadProperties getInstance() {
        if (instance == null) {
            instance = new DownloadProperties();
        }
        return instance;
    }

    public boolean isLogForManagerActive() {
        return logforManagerflag;
    }

    public void setLogforManagerflag(boolean logforManagerflag) {
        this.logforManagerflag = logforManagerflag;
    }

}
