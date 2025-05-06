/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.connection.settings.FileInfoEnum;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author EnesTerzi
 */
public class RemoteFileObject extends FileObject {

    private final SimpleDateFormat remoteFileformat = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'EEST' yyyy");
    private final SimpleDateFormat remoteFileformat2 = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'EET' yyyy");
    private final SimpleDateFormat remoteFileformat3 = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'GMT' yyyy");
    private final SimpleDateFormat remoteFileformat4 = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'TRT' yyyy");
    private final SimpleDateFormat remoteFileformat5 = new SimpleDateFormat("EEE MMM dd HH:mm:ss 'UTC' yyyy");   //Sat Jun 08 16:11:19 GMT 2019

    public RemoteFileObject(FileInfoEnum fileinfo) {
        super(fileinfo);
    }

    public void setDate(Object obj) {
        try {
            super.fileCreatedDate = (Date) obj;

            //Sftp date string gelir
        } catch (ClassCastException ex) {
            String date = (String) obj;
            SimpleDateFormat defaultFormatted = null;
            if (date.contains("EET")) {
                defaultFormatted = remoteFileformat2;
            } else if (date.contains("EEST")) {
                defaultFormatted = remoteFileformat;
            } else if (date.contains("GMT")) {
                defaultFormatted = remoteFileformat3;
            } else if (date.contains("TRT")) {
                defaultFormatted = remoteFileformat4;
            } else if (date.contains("UTC")) {
                defaultFormatted = remoteFileformat5;
            } else {
                System.out.println("Getting Ftp file date problem...");
                System.err.println("Getting Ftp file date problem...");
            }

            try {
                super.fileCreatedDate = defaultFormatted.parse(date);
            } catch (Exception e) {
            }
        }

    }
}
