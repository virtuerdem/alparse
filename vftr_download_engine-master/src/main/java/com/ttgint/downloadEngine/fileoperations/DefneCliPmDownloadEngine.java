package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 *
 * @author ibrahimegerci
 */
@DownloadEngine(systemType = "DEFNE_CLI", measType = "PM", operatorName = "VODAFONE")
public class DefneCliPmDownloadEngine extends DefaultTimeBasedEngine {

    public DefneCliPmDownloadEngine(ServerIpList eachIp) {
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

        HashMap<String, FileHousekeep> reDownloadList = new HashMap<>();
        for (FileHousekeep each : fromHousekeepReDownloadList) {
            //Add file for ReDownloadList
            reDownloadList.put(each.getFileName(), each);
        }
        System.out.println(connectionInfo.getIp() + ":" + connectionInfo.getRemotePath() + " From file Housekeep size : " + reDownloadList.size());

        List<Map> fileNameFilters = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableListActive(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));

        HashSet<String> fileNameKeys = new HashSet<>();
        for (Map fileNameFilter : fileNameFilters) {
            if (!fileNameFilter.get("FUNCTIONSUBSETNAME").toString().isBlank()) {
                fileNameKeys.add((String) fileNameFilter.get("FUNCTIONSUBSETNAME"));
            }
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        for (RemoteFileObject each : fileList) {
            try {
                //NeName Filter,  File Source Path Check
                if (each.getFileName().startsWith("kpi.csv.")
                        && fileNameKeys.contains(
                                each.getAbsolutePath().substring(1, each.getAbsolutePath().length()).replace("/", "_"))) {

                    String fileName = connectionInfo.getIp() + "+"
                            + each.getAbsolutePath().substring(1, each.getAbsolutePath().length()).replace("/", "_") + "+"
                            + each.getFileName() + ".csv";

                    //File Date Check Cause of ReCreated Files
                    try {
                        calendar.setTime(new SimpleDateFormat("yyyy-MM-dd-HH")
                                .parse(fileName.split("\\+")[2].split("\\.")[2].substring(0, 13)));
                        if (fileName.endsWith("-12-1.csv")) {
                            calendar.add(Calendar.HOUR_OF_DAY, -12);
                        } else if (!fileName.endsWith("-12-2.csv")
                                && fileName.endsWith("-2.csv")) {
                            calendar.add(Calendar.HOUR_OF_DAY, 12);
                        }
                    } catch (ParseException ex) {
                    }

                    if (new SimpleDateFormat("yyyy-MM-dd-HH").format(new Date())
                            .equals(new SimpleDateFormat("yyyy-MM-dd-HH").format(calendar.getTime()))) {
                        continue;
                    }

                    //ReDownload
                    if (reDownloadList.containsKey(fileName)) {
                        boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + fileName, each.getPath());

                        dbOperationExistFileObject(reDownloadList.get(fileName), downloadStatus, each.getPath());
                        reDownloadList.remove(fileName);
                        continue;
                    }

                    //TimeBased Download
                    if (each.getDate().after(getLastDateFromdb())) {
                        boolean downloadStatus = con.downloadFile(DownloadApp.LOCALFILEPATH + fileName, each.getPath());
                        
                        FileHousekeep fileHousekeepObj = new FileHousekeep();
                        fileHousekeepObj.setDownloaded(downloadStatus ? 1 : 0);
                        fileHousekeepObj.setDownloadTryCount(1);
                        fileHousekeepObj.setFileDate(calendar.getTime());
                        fileHousekeepObj.setFileName(fileName);
                        fileHousekeepObj.setFileSize(each.getFileSize());
                        fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
                        fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                        fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                        fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                        fileHousekeepObj.setFileCreatedDate(each.getDate());

                        dbOperationNonExistFileObject(fileHousekeepObj, each);
                    }
                }
            } catch (Exception e) {
                continue;
            }
        }

        for (FileHousekeep fileHousekeep : reDownloadList.values()) {
            dbOperationExistFileObject(fileHousekeep, false, null);
        }
    }

}
