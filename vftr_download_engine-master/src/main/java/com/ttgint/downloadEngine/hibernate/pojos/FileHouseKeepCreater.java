/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.hibernate.pojos;

import java.util.Date;

/**
 *
 * @author TTGETERZI
 */
public  class FileHouseKeepCreater {

    private String fileName;
    private Integer downloaded;
    private Integer downloadTryCount;
    private String systemType;
    private Long fileSize;
    private String operatorName;
    private Date fileDate;
    private Integer connection;
    private Date fileCreatedDate;

    private static FileHouseKeepCreater currentObject;

    public static FileHouseKeepCreater create() {
        currentObject = new FileHouseKeepCreater();
        return currentObject;
    }

    public FileHouseKeepCreater setFileName(String fileName) {
        this.fileName = fileName;
        return currentObject;
    }

    public FileHouseKeepCreater setDownloaded(Integer downloaded) {
        this.downloaded = downloaded;
        return currentObject;
    }

    public FileHouseKeepCreater setDownloadTryCount(Integer downloadTryCount) {
        this.downloadTryCount = downloadTryCount;
        return currentObject;
    }

    public FileHouseKeepCreater setSystemType(String systemType) {
        this.systemType = systemType;
        return currentObject;
    }

    public FileHouseKeepCreater setFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return currentObject;
    }

    public FileHouseKeepCreater setOperatorName(String operatorName) {
        this.operatorName = operatorName;
        return currentObject;
    }

    public FileHouseKeepCreater setFileDate(Date fileDate) {
        this.fileDate = fileDate;
        return currentObject;
    }

    public Integer getConnection() {
        return connection;
    }

    public FileHouseKeepCreater setConnectionId(Integer connection) {
        this.connection = connection;
        return currentObject;
    }
     public FileHouseKeepCreater setFileCreatedDate(Date fileCreatedDate) {
        this.fileCreatedDate = fileCreatedDate;
        return currentObject;
    }
    
    

    public FileHousekeep build() {
        FileHousekeep houseObj
                = new FileHousekeep(fileName, downloaded, downloadTryCount, systemType, fileSize, operatorName, fileDate,connection,fileCreatedDate);
        return houseObj;
    }

}
