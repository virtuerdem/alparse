/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import com.ttgint.downloadEngine.connection.settings.FileInfoEnum;
import java.util.Date;

/**
 *
 * @author TurgutSimsek
 */
public class LocalFileObject extends FileObject {

    public LocalFileObject(FileInfoEnum fileinfo) {
        super(fileinfo);
    }

    public void setDate(Date date) {
        super.fileCreatedDate = date;
    }
    
}
