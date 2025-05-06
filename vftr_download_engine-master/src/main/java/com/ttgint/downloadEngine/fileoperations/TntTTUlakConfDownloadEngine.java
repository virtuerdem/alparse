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
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "ULAK4G-CONF", measType = "CM", operatorName = "TURKTELEKOM")})
public class TntTTUlakConfDownloadEngine extends DefaultTimeBasedEngine {

    public TntTTUlakConfDownloadEngine(ServerIpList eachIp) {
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
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                if (!(each.getFileName().startsWith("TT_TNT"))) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String eachFileName = each.getFileName().replace(":", "-");
            String fullLocalFileName = DownloadApp.LOCALFILEPATH + eachFileName;

            //ReDownload
            if (reDownloadList.containsKey(eachFileName)) {
                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());

                if (downloadStatus) {
                    if (fullLocalFileName.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(eachFileName), downloadStatus, each.getPath());
                reDownloadList.remove(eachFileName);
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());

                if (downloadStatus) {
                    if (each.getFileName().endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                            ex.printStackTrace();
                        }
                    }
                }

                Date fileDate = new Date();
                try {
                    String eachFileNameSplit = eachFileName.split("\\.", 2)[0];
                    fileDate = new SimpleDateFormat("dd_MM_YYYY").parse(eachFileNameSplit.substring(eachFileNameSplit.length() - 10, eachFileNameSplit.length()));
                } catch (ParseException ex) {
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(fileDate);
                fileHousekeepObj.setFileName(eachFileName);
                fileHousekeepObj.setFileSize(each.getFileSize());
                fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setFileCreatedDate(each.getDate());

                dbOperationNonExistFileObject(fileHousekeepObj, each);

            }
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }

}
