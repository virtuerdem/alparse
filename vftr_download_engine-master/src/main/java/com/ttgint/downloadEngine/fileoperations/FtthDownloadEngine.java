package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.LocalFileLib;
import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import com.ttgint.downloadEngine.engines.CopyEngine;
import com.ttgint.downloadEngine.engines.UnzipOperation;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 *
 * @author abdullah.yakut
 */
@DownloadEngine(systemType = "ALUFTTH", measType = "PM", operatorName = "VODAFONE")
public class FtthDownloadEngine extends CopyEngine {

    public FtthDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void copyFiles(Date lastDateFromDb, LocalFileLib fileLib, List<LocalFileObject> files, ServerIpList connectionInfo) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

        List<FileHousekeep> fromHousekeepReDownloadList = DaoUtils.getObject(FileHousekeep.class, "standartDownloadQueryTimeBased",
                new String[]{"systemType", "connection"}, new Object[]{connectionInfo.getSystemType(), connectionInfo.getConnectionId()});

        for (FileHousekeep fileHousekeep : fromHousekeepReDownloadList) {
            boolean flag = false;
            for (LocalFileObject each : files) {
                flag = fileHousekeep.getFileName().equals(each.getFileName());
                if (flag) {
                    String folderName = connectionInfo.getRemotePath().split("/")[connectionInfo.getRemotePath().split("/").length - 1];
                    File targetFile = new File(DownloadApp.LOCALFILEPATH + folderName + "-" + each.getFileName());
                    dbOperationExistFileObject(fileHousekeep, fileLib.copyFile(targetFile.getPath(), each.getPath()), each.getPath());
                    try {
                        UnzipOperation.uncompressTarGzInFolder(targetFile, new File(DownloadApp.LOCALFILEPATH + targetFile.getName().replace(".tar.gz", "")));
                    } catch (IOException ex) {
                    }
                    break;
                }
            }
            if (flag == false) {
                dbOperationExistFileObject(fileHousekeep, false, null);
            }
        }

        for (LocalFileObject each : files) {
            String folderName = connectionInfo.getRemotePath().split("/")[connectionInfo.getRemotePath().split("/").length - 1];
            File targetFile = new File(DownloadApp.LOCALFILEPATH + folderName + "-" + each.getFileName());

            if (each.getFileName().endsWith(".tar.gz") && each.getDate().after(lastDateFromDb)) {

                fileLib.copyFile(targetFile.getPath(), each.getPath());

                try {
                    String fileDate = each.getFileName().split("_")[2].replace(".tar.gz", "").replace("-", "");
                    if (fileDate.length() <= 10) {
                        fileDate += "00";
                    }

                    UnzipOperation.uncompressTarGzInFolder(targetFile, new File(DownloadApp.LOCALFILEPATH + targetFile.getName().replace(".tar.gz", "")));

                    FileHousekeep fileHouseKeep = new FileHousekeep();
                    fileHouseKeep.setDownloaded(1);
                    fileHouseKeep.setDownloadTryCount(1);
                    fileHouseKeep.setFileSize(each.getFileSize());
                    fileHouseKeep.setOperatorName(DownloadApp.OPERATORNAME);
                    fileHouseKeep.setFileDate(sdf.parse(fileDate));
                    fileHouseKeep.setSystemType(DownloadApp.SYSTEMTYPE);
                    fileHouseKeep.setMeasType(DownloadApp.MEASTYPE);
                    fileHouseKeep.setFileName(each.getFileName());
                    fileHouseKeep.setConnectionId(connectionInfo.getConnectionId());

                    dbOperationNonExistFileObject(fileHouseKeep, each);

                    targetFile.delete();

                } catch (ParseException | IOException ex) {
                    System.out.println("DateParseException " + each.getFileName() + ": " + ex.getMessage());
                }
            }
        }
    }
}
