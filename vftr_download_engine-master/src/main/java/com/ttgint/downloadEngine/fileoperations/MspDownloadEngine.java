package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.connection.settings.ConnectionLibs;
import com.ttgint.downloadEngine.engines.DefaultTimeBasedEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.KpiList;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "MSP", measType = "PM", operatorName = "VODAFONE")
public class MspDownloadEngine extends DefaultTimeBasedEngine {

    public MspDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void setConnectionInfoLib(ConnectionInfo info) {
        info.setLib(info.getPort() == 21 ? ConnectionLibs.ApacheLibFTP : ConnectionLibs.ApacheLibSFTP);
    }

    @Override
    public void onDownlaod(Connection con, ServerIpList connectionInfo, List<RemoteFileObject> fileList) {

        List<KpiList> activeKpiList = DaoUtils.getObject(KpiList.class, "allActiveKpiListNe",
                new String[]{"operatorName", "systemType", "measType", "neType"},
                new Object[]{DownloadApp.OPERATORNAME, DownloadApp.SYSTEMTYPE, DownloadApp.MEASTYPE, connectionInfo.getElementManager()});

        HashSet<String> activeKpiNames = new HashSet<>();
        for (KpiList each : activeKpiList) {
            activeKpiNames.add("|" + each.getKpiName() + "|");
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR, -1);
        Date fileDate = cal.getTime();
        try {
            fileDate = new SimpleDateFormat("yyyyMMddHH").parse(new SimpleDateFormat("yyyyMMddHH").format(fileDate));
        } catch (Exception e) {
        }

        for (RemoteFileObject each : fileList) {
            try {
                String kpi = each.getFileName()
                        .replace(each.getFileName().split("\\-")[0] + "-", "")
                        .replace(each.getFileName().split("\\-")[1] + "-", "")
                        .replace(".rrd", "");

                if (each.getFileName().endsWith(".rrd") && each.getDate().after(getLastDateFromdb()) && activeKpiNames.contains("|" + kpi + "|")) {
                    String fullPath = DownloadApp.LOCALFILEPATH + each.getFileName();
                    boolean downloadStatus = con.downloadFile(fullPath, each.getPath());

                    if (downloadStatus) {
                        downloadStatus = decodeFile(fullPath);
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
            } catch (Exception e) {
                continue;
            }
        }
    }

    private boolean decodeFile(String fullPath) {
        boolean uncompress = true;
        if (new File(fullPath).exists()) {
            try {
                Process proc = Runtime.getRuntime().exec(String.format("/northi/NorthiParserEngine/rrdtool dump %s %s", fullPath, fullPath.replace(".rrd", ".xml")));
                proc.waitFor();
                new File(fullPath).delete();
            } catch (IOException | InterruptedException ex) {
                new File(fullPath).delete();
                uncompress = false;
            }
        } else {
            uncompress = false;
        }
        return uncompress;
    }
}
