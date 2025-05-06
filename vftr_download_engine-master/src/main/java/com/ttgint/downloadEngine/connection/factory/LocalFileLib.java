/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.connection.settings.FileInfoEnum;
import com.ttgint.downloadEngine.connection.settings.FileListener;
import com.ttgint.downloadEngine.connection.settings.LocalCommand;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 *
 * @author TurgutSimsek
 */
public class LocalFileLib implements LocalCommand {

    @Override
    public List<LocalFileObject> readAllFilesWalkinPath(String remotePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public boolean copyFile(String fullLocalPad, String fullRemotePath) {

        boolean copyStatus = false;
        try {
            Files.copy(new File(fullRemotePath).toPath(), new File(fullLocalPad).toPath());
            copyStatus = true;
        } catch (IOException ex) {
            System.out.println("Error copy " + fullRemotePath + ": " + ex.getMessage());
        }
        return copyStatus;
    }

    @Override
    public List<LocalFileObject> readAllFilesInCurrentPath(String remotePath) {
        List<LocalFileObject> willReturnObject = new ArrayList<>();

        File[] fileListTemp = new File(remotePath).listFiles();
        for (File each : fileListTemp) {
            try {
                LocalFileObject objectTemp = null;
                if (each.isFile()) {

                    objectTemp = new LocalFileObject(FileInfoEnum.FILE);
                    objectTemp.setFileName(each.getName());
                    objectTemp.setAbsolutePath(remotePath);
                    objectTemp.setFileSize(each.length());
                    objectTemp.setFileType();

                    Calendar cal = Calendar.getInstance();
                    cal.setTimeInMillis(each.lastModified());
                    objectTemp.setDate(cal.getTime());
                    willReturnObject.add(objectTemp);
                }
            } catch (NullPointerException ex) {
            }
        }
        return willReturnObject;
    }

    @Override
    public void readAllFilesWalkingPathWithListener(FileListener listener, String remotePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void readAllFilesInCurrentPathWithListener(FileListener listener, String remotePath) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
