/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.settings;

/**
 *
 * @author TTGETERZI
 */
public interface ConnectionListener {

    public  void downloadStatus(String fileName, boolean downloadStatus);

}
