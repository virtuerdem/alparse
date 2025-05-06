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
 * @author TurgutSimsek
 */
public abstract class FileObject {

    private String fileName;
    private String path;
    private String absolutePath;
    protected Date fileCreatedDate;
    private long fileSize;
    private String fileType;
    private final FileInfoEnum fileinfo;

    public FileInfoEnum getFileInfo() {
        return fileinfo;
    }

    public FileObject(FileInfoEnum fileinfo) {
        this.fileinfo = fileinfo;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setAbsolutePath(String path) {
        this.absolutePath = path;
        setPath();
    }

    private void setPath() {
        this.path = (this.absolutePath + "/" + this.fileName).replace("//", "/");
    }

    public void setDirectPath(String path) {
        this.path = path;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public void setFileType() {
        String[] splitted = fileName.split("\\.");
        fileType = splitted[splitted.length - 1];
    }

    public String getFileName() {
        return fileName;
    }

    public String getPath() {
        return path;
    }

    public String getAbsolutePath() {
        return absolutePath;
    }

    public Date getDate() {
        return fileCreatedDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getFileType() {
        return fileType;
    }

    public String getFileDateWithPattern(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(fileCreatedDate);
    }

}
