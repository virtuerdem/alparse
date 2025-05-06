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
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Turgut Simsek
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "HWU2000", measType = "PM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HWU2000", measType = "PM", operatorName = "KKTC-TELSIM")
})
public class U2000DownloadEngine extends DefaultTimeBasedEngine {

    public U2000DownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownload(Connection con, ServerIpList connectionInfo) {
        if (!con.getConnection()) {
            return;
        }
        setCommitSize(10);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

        HashSet<String> pathList = new HashSet<>();
        Calendar cal = Calendar.getInstance();
        //Add CurrentHour for Path
        pathList.add(sdf.format(cal.getTime()));
        //Add PreHours for Path
        cal.add(Calendar.DATE, -1);
        pathList.add(sdf.format(cal.getTime()));
        cal.add(Calendar.DATE, -1);
        pathList.add(sdf.format(cal.getTime()));

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + fromHousekeepReDownloadList.size());

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
            //Add ReDownloadDate for Path
            pathList.add(each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 2].substring(0, 8));
        }

        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " SubPath size : " + pathList.size());
        List<RemoteFileObject> fileList = new ArrayList<>();
        for (String eachDate : pathList) {
            String remotePath = (connectionInfo.getRemotePath() + "/" + eachDate + "/").replace("//", "/");
            fileList.addAll(con.readAllFilesWalkinPath(remotePath));
        }

        HashSet<String> fileNames = new HashSet<>();
        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetIdAndTableNameFromParserRawTableList(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        for (Map fileNameFilter : fileNameFilters) {
            if (!fileNameFilter.get("FUNCTIONSUBSET_ID").toString().isBlank()) {
                fileNames.add((String) fileNameFilter.get("FUNCTIONSUBSET_ID"));
            }
        }

        for (RemoteFileObject each : fileList) {
            //File Filter
            try {
                String fileName = each.getFileName().split("\\_")[1];
                if (!fileNames.contains(fileName)) {
                    continue;
                }
            } catch (Exception e) {
                continue;
            }

            String localFileName = connectionInfo.getElementManager().split("\\-")[1] + "-" + connectionInfo.getIp() + ";" + each.getFileName();
            String fullLocalFileName = DownloadApp.LOCALFILEPATH + localFileName;

            //ReDownload
            if (reDownloadList.containsKey(localFileName)) {
                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());
                if (downloadStatus) {
                    if (fullLocalFileName.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                            System.err.println("* UnGz File Error: " + each.getFileName());
                        }
                    }
                }

                dbOperationExistFileObject(reDownloadList.get(localFileName), downloadStatus, each.getPath());
                reDownloadList.remove(localFileName);
                continue;
            }

            //TimeBased Download
            if (each.getDate().after(getLastDateFromdb())) {

                Date fileDate = new Date();
                try {
                    fileDate = new SimpleDateFormat("yyyyMMddHHmm").parse(each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 2].substring(0, 12));
                } catch (ParseException ex) {
                }

                boolean downloadStatus = con.downloadFile(fullLocalFileName, each.getPath());
                if (downloadStatus) {
                    if (fullLocalFileName.endsWith(".gz")) {
                        try {
                            downloadStatus = UnzipOperation.unzipGzFile(fullLocalFileName);
                        } catch (IOException ex) {
                            downloadStatus = false;
                            System.err.println("* UnGz File Error: " + each.getFileName());
                        }
                    }
                }

                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(fileDate);
                fileHousekeepObj.setFileName(localFileName);
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
