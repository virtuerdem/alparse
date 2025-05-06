/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
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
@DownloadEngine(systemType = "E3G-CONF", measType = "CM", operatorName = "TURKTELEKOM")
public class TntTTE3gConfDownloadEngine extends DefaultTimeBasedEngine {

    private Date fileDate;

    public TntTTE3gConfDownloadEngine(ServerIpList eachIp) {
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
                String fullLocalath = DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "_" + each.getFileName();
                if (flag) {
                    dbOperationExistFileObject(fileHousekeep, con.downloadFile(fullLocalath, each.getPath()), each.getPath());

                    flag = UnzipOperation.unzipZipFileToSubDirectory(fullLocalath);
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (RemoteFileObject each : fileList) {

            if (each.getDate().after(getLastDateFromdb()) && each.getFileName().endsWith(".zip") && each.getFileName().contains("UTRAN_TOPOLOGY_BOTH")) {

                boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "_" + each.getFileName(), each.getPath());

                if (downloadStatus) {
                    downloadStatus = UnzipOperation.unzipZipFileToSubDirectory(DownloadApp.LOCALFILEPATH + connectionInfo.getIp() + "_" + each.getFileName());
                }

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    //UTRAN_TOPOLOGY_BOTH_201810150500.zip
                    String fileDateStr = each.getFileName().split("\\BOTH_")[1];
                    fileDateStr = fileDateStr.replace(".zip", "");
                    fileDateStr = fileDateStr.substring(0, fileDateStr.length() - 4);
                    fileDate = sdf.parse(fileDateStr);

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
