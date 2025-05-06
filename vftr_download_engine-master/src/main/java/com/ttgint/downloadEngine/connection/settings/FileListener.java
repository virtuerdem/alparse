/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.settings;

import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;

/**
 *
 * @author TTGETERZI
 */
public interface FileListener {
    
    void handleRemoteFile(RemoteFileObject Object);

}
