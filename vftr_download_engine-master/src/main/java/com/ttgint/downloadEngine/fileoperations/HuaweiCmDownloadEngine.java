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
import com.ttgint.downloadEngine.engines.DefaultStandartEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ParserUsedNes;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author TTGETERZI
 */
@DownloadEngines(downloadEngines = {
    @DownloadEngine(systemType = "HW5G", measType = "CM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW4G", measType = "CM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW3G", measType = "CM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW2G", measType = "CM", operatorName = "VODAFONE"),
    @DownloadEngine(systemType = "HW4G", measType = "CM", operatorName = "KKTC-TELSIM"),
    @DownloadEngine(systemType = "HW3G", measType = "CM", operatorName = "KKTC-TELSIM"),
    @DownloadEngine(systemType = "HW2G", measType = "CM", operatorName = "KKTC-TELSIM")})
public class HuaweiCmDownloadEngine extends DefaultStandartEngine {

    List<String> neededNeNamesList;

    public HuaweiCmDownloadEngine(ServerIpList eachIp) {
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

        Calendar cal = Calendar.getInstance();
        String today = new SimpleDateFormat("yyyyMMdd").format(cal.getTime());

        getNeededNeNames(connectionInfo, today);

        List<RemoteFileObject> remoteFullFileList
                = filterFilesByCreatedTime(con.readAllFilesInCurrentPath(connectionInfo.getRemotePath()), today);
        for (RemoteFileObject each : remoteFullFileList) {
            if (neededNeNamesList.contains(each.getFileName().replace(each.getFileName().split("\\_")[0] + "_", "")
                    .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 2], "")
                    .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1], ""))) {
                switch (DownloadApp.SYSTEMTYPE) {
                    case "HW5G":
                    case "HW4G":
                        if ((each.getFileName().startsWith("CMExport_SR")
                                || each.getFileName().startsWith("CMExport_GL")
                                || each.getFileName().startsWith("CMExport_GU")
                                || each.getFileName().startsWith("CMExport_NR"))) {
                            downloadAndUncopmresssFile(each, con, connectionInfo);
                        }
                        break;
                    case "HW3G":
                        if (each.getFileName().startsWith("CMExport_R")) {
                            downloadAndUncopmresssFile(each, con, connectionInfo);
                        }
                        break;
                    case "HW2G":
                        if (each.getFileName().startsWith("CMExport_B")) {
                            downloadAndUncopmresssFile(each, con, connectionInfo);
                        }
                        break;
                }
            }
        }

//        if (DownloadApp.OPERATORNAME.equals("VODAFONE") && DownloadApp.SYSTEMTYPE.equals("HW4G")) {
//            List<String> activeNeNames = getActiveNeNames(connectionInfo.getIp());
//            for (RemoteFileObject each : remoteFullFileList) {
//                try {
//                    if (!activeNeNames.contains(each.getFileName()
//                            .replace(each.getFileName().split("\\_")[0] + "_", "")
//                            .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1], "")
//                            .replace("_" + each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 2], ""))
//                            && (each.getFileName().startsWith("CMExport_SR")
//                            || each.getFileName().startsWith("CMExport_GL")
//                            || each.getFileName().startsWith("CMExport_GU")
//                            || each.getFileName().startsWith("CMExport_NR"))) {
//                        Date fileDateObj = new Date();
//                        try {
//                            fileDateObj = new SimpleDateFormat("yyyyMMddHH").parse(each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1].substring(0, 10));
//                        } catch (ParseException ex) {
//                        }
//
//                        FileHousekeep fileHousekeepObj = new FileHousekeep();
//                        fileHousekeepObj.setDownloaded(1);
//                        fileHousekeepObj.setDownloadTryCount(11);
//                        fileHousekeepObj.setFileDate(fileDateObj);
//                        fileHousekeepObj.setFileName(each.getFileName());
//                        fileHousekeepObj.setFileSize(each.getFileSize());
//                        fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
//                        fileHousekeepObj.setSystemType("MISS-" + DownloadApp.SYSTEMTYPE);
//                        fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
//                        fileHousekeepObj.setConnectionId(connectionInfo.getConnectionId());
//                        fileHousekeepObj.setFileCreatedDate(each.getDate());
//
//                        dbOperationNonExistFileObject(fileHousekeepObj, each);
//
//                    }
//                } catch (Exception e) {
//                }
//            }
//        }
    }

    @Override
    public void afterFinishForCurrentThread(Connection con) {
        if (con != null) {
            if (con.checkConnectionisAlive()) {
                con.closeConnection();
            }
        }
    }

    private void getNeededNeNames(ServerIpList connectionInfo, String date) {
        /*
         Raw object tablosu incelenir . O gun icindeki data icerisinde bulunmayan aktif ne name leri bulur.

         */
        neededNeNamesList = new ArrayList<>();
        List<Map> list = DaoUtils.getQueryAsListMap(DownloadQueries.
                downloadQueryForHuaweiCm(DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME, date, connectionInfo.getIp()));
        for (Map each : list) {
            String neName = (String) each.get("NE_NAME");
            neededNeNamesList.add(neName);
        }
    }

    private List<String> getActiveNeNames(String ip) {
        String[] parameters = {"systemType", "operatorName", "m2000ServerIpCms"};
        Object[] values = {DownloadApp.SYSTEMTYPE, DownloadApp.OPERATORNAME, ip};
        List<ParserUsedNes> getIpList
                = DaoUtils.getObject(
                        ParserUsedNes.class,
                        "hwcmNeNameQuery",
                        parameters,
                        values);

        return getIpList.stream().map(ParserUsedNes::getNeName).collect(Collectors.toList());
    }

    private List<RemoteFileObject> filterFilesByCreatedTime(List<RemoteFileObject> remoteFullFileList, String checkDate) {
        HashMap<String, RemoteFileObject> distinctedRemoteFileList = new HashMap<>();
        for (RemoteFileObject rfo : remoteFullFileList) {
            if (rfo.getFileName().contains(checkDate)) {
                try {
                    String neName = rfo.getFileName()
                            .replace(rfo.getFileName().split("\\_")[0] + "_", "")
                            .replace("_" + rfo.getFileName().split("\\_")[rfo.getFileName().split("\\_").length - 1], "")
                            .replace("_" + rfo.getFileName().split("\\_")[rfo.getFileName().split("\\_").length - 2], "");
                    if (!distinctedRemoteFileList.containsKey(neName)) {
                        distinctedRemoteFileList.put(neName, rfo);
                    } else {
                        if (rfo.getDate().after(distinctedRemoteFileList.get(neName).getDate())) {
                            distinctedRemoteFileList.put(neName, rfo);
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
        return new ArrayList(distinctedRemoteFileList.values());
    }

    private void downloadAndUncopmresssFile(RemoteFileObject each, Connection con, ServerIpList connectionInfo) {
        String fullLocal = DownloadApp.LOCALFILEPATH + each.getFileName();
        boolean status = con.downloadFile(fullLocal, each.getPath());

        if (status && each.getFileName().endsWith(".gz")) {
            try {
                UnzipOperation.unzipGzFile(fullLocal);
            } catch (IOException ex) {
                System.err.println("Unzip Error: " + connectionInfo.getIp() + " " + each.getFileName());
                new File(fullLocal).delete();
                return;
            }
        }

        Date fileDateObj = new Date();
        try {
            fileDateObj = new SimpleDateFormat("yyyyMMddHH").parse(each.getFileName().split("\\_")[each.getFileName().split("\\_").length - 1].substring(0, 10));
        } catch (ParseException ex) {
        }

        FileHousekeep fileHousekeepObj = new FileHousekeep();
        fileHousekeepObj.setDownloaded(status ? 1 : 0);
        fileHousekeepObj.setDownloadTryCount(1);
        fileHousekeepObj.setFileDate(fileDateObj);
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
