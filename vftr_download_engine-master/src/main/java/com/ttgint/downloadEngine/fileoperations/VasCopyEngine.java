/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.LocalFileLib;
import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import com.ttgint.downloadEngine.engines.CopyEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author abdullah.yakut
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "VAS", measType = "PM", operatorName = "VODAFONE")})
public class VasCopyEngine extends CopyEngine {

    public VasCopyEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void copyFiles(Date lastDateFromDb, LocalFileLib fileLib, List<LocalFileObject> files, ServerIpList connectionInfo) {

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (LocalFileObject each : files) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, fileLib.copyFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }
        for (LocalFileObject each : files) {

            if (each.getFileName().endsWith(".csv") && each.getFileName().contains("_report")) {

                if (each.getDate().after(lastDateFromDb)) {

                    boolean copyStatus = fileLib.copyFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                    FileHousekeep fileHousekeepObj = new FileHousekeep();
                    fileHousekeepObj.setDownloaded(copyStatus ? 1 : 0);
                    fileHousekeepObj.setDownloadTryCount(1);
                    fileHousekeepObj.setFileSize(each.getFileSize());
                    fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHousekeepObj.setFileDate(getFileDate(each.getFileName()));
                    fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                    fileHousekeepObj.setFileName(each.getFileName());
                    fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                    fileHousekeepObj.setFileCreatedDate(each.getDate());

                    dbOperationNonExistFileObject(fileHousekeepObj, each);

                }
            }
        }
    }

    private Date getFileDate(String fileName) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat sdfKpi = new SimpleDateFormat("yyyyMMdd-HHmmss");
        String fileDates = "";
        Date fileDate = null;

        if (DownloadApp.SYSTEMTYPE.equals("VAS")) {
            fileDates = fileName.split("\\.")[0].replace("-", "");
            try {
                fileDate = sdf.parse(fileDates);
            } catch (ParseException ex) {
                Logger.getLogger(PcrfCopyEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            fileDates = fileName.split("\\_")[1];
            String[] dt = fileDates.split("\\-");

            fileDates = dt[0] + "-" + dt[1];
            try {
                fileDate = sdfKpi.parse(fileDates);
            } catch (ParseException ex) {
                Logger.getLogger(PcrfCopyEngine.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return fileDate;
    }

}
