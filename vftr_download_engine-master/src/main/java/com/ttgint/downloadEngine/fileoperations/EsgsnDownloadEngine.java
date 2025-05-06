package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.IOException;
import java.util.HashMap;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngine(systemType = "ESGSN", measType = "PM", operatorName = "VODAFONE")
public class EsgsnDownloadEngine extends DefaultTimeBasedEngine {

    public EsgsnDownloadEngine(ServerIpList eachIp) {
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

        List<String> listNeName = DaoUtils.getObject(String.class, "getNeBySystem", new String[]{"systemType", "operatorName"},
                new Object[]{DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME});

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
        }

        for (RemoteFileObject each : fileList) {
            //NeName Filter
            try {
                String neNamePath = "=" + each.getPath().split("\\/")[each.getPath().split("\\/").length - 2] + ",";
                boolean fileFilter = false;
                for (String neName : listNeName) {
                    if (neNamePath.contains("=" + neName + ",")) {
                        fileFilter = true;
                        break;
                    }
                }

                if (!fileFilter) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            //ReDownload
            if (reDownloadList.containsKey(each.getFileName())) {
                String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    if (localFileName.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(localFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(each.getFileName()), downloadStatus, each.getPath());
                reDownloadList.remove(each.getFileName());
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {
                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMdd.HHmm").parse(each.getFileName().substring(1, 14));
                } catch (ParseException ex) {
                }

                String localFileName = DownloadApp.LOCALFILEPATH + each.getFileName();
                boolean downloadStatus = con.downloadFile(localFileName, each.getPath());

                if (downloadStatus) {
                    if (localFileName.endsWith(".gz")) {
                        try {
                            UnzipOperation.unzipGzFile(localFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                        }
                    }
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

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }
}
