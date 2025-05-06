/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author turgut.simsek
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "HW-CSR", measType = "CM", operatorName = "TURKTELEKOM"),
    @DownloadEngine(systemType = "ALCATEL-CSR", measType = "CM", operatorName = "TURKTELEKOM")})
public class TntAluHwCsrDownloadEngine extends DefaultTimeBasedEngine {

    private String fileDateStr;
    private Date fileDate;

    public TntAluHwCsrDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (RemoteFileObject each : fileList) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath()), each.getPath());
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {

            if (each.getDate().after(getLastDateFromdb()) && (each.getFileName().endsWith(".csv") || each.getFileName().endsWith(".CSV"))) {

                boolean hwDownloadFlag = (DownloadApp.SYSTEMTYPE.equals("HW-CSR") && each.getFileName().startsWith("HUA"));
                boolean aluDownloadFlag = (DownloadApp.SYSTEMTYPE.equals("ALCATEL-CSR") && each.getFileName().startsWith("ALU"));

                if (hwDownloadFlag || aluDownloadFlag) {

                    boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + each.getFileName(), each.getPath());

                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy");
                        String[] fileNameArr = each.getFileName().split("\\_");
                        if (each.getFileName().contains("_PAN")) {
                            fileDateStr = fileNameArr[3].split("\\.")[0];
                            fileDateStr = fileDateStr.substring(0, 8);
                            fileDate = sdf.parse(fileDateStr);
                        } else {

                            fileDateStr = fileNameArr[1].split("\\.")[0];
                            fileDateStr = new SimpleDateFormat("yyyyMMdd").format(sdf.parse(fileDateStr));
                            fileDate = sdf.parse(fileDateStr);
                        }
                    } catch (ParseException ex) {
                    }

                    FileHousekeep fileHousekeepObj = new FileHousekeep();
                    fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                    fileHousekeepObj.setDownloadTryCount(1);
                    fileHousekeepObj.setFileDate(fileDate);
                    fileHousekeepObj.setFileName(each.getFileName());
                    fileHousekeepObj.setFileSize(each.getFileSize());
                    fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                    fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                    fileHousekeepObj.setFileCreatedDate(each.getDate());

                    dbOperationNonExistFileObject(fileHousekeepObj, each);

                }
            }
        }
    }
}
