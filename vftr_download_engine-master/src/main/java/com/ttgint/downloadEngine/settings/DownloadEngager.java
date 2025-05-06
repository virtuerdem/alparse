/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.settings;

import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author EnesTerzi
 */
public final class DownloadEngager {

    private final Class<?> classObject;

    public DownloadEngager(Class<?> classObject) {
        this.classObject = classObject;
    }

    private void checkLocalFilePath() {
        File file = new File(DownloadApp.LOCALFILEPATH);
        if (file.exists() == false) {
            file.mkdirs();
        }
    }

    public void engageDownload() throws InstantiationException,
            IllegalAccessException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {

        checkLocalFilePath();
        engageThreadDownloadEngines();
    }

    public void engageThreadDownloadEngines() throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ExecutorService executors = Executors.newFixedThreadPool(DownloadApp.threadCount);
        System.out.println("Active ip size : " + DownloadApp.currentSystemIpList.size());

        for (ServerIpList each : DownloadApp.currentSystemIpList) {
            executors.execute((Runnable) classObject.getDeclaredConstructor(ServerIpList.class).newInstance(each));
        }

        executors.shutdown();
        while (!executors.isTerminated()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
        }
    }

}
