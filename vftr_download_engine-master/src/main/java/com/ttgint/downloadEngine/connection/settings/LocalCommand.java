/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.settings;

import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import java.util.List;

/**
 *
 * @author TurgutSimsek
 */
public interface LocalCommand {
    
    List<LocalFileObject> readAllFilesWalkinPath(String remotePath);

    List<LocalFileObject> readAllFilesInCurrentPath(String remotePath);

    void readAllFilesWalkingPathWithListener(FileListener listener,String remotePath);
    
    void readAllFilesInCurrentPathWithListener(FileListener listener,String remotePath);
    
}
