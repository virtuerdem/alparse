/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.ConnectionFactory;
import com.ttgint.downloadEngine.connection.factory.FileObject;
import com.ttgint.downloadEngine.connection.factory.RemoteFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import java.io.File;
import java.util.Date;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author EnesTerzi
 */

/*
 Bu sinif download enginlarin super classidir . Tum main islemler burada otomatik olarak gerceklestirilir.
 Burada herhangi bir degisiklik yapmaya kesinlikle !!! gerek yoktur. 
 Yeni bir engine yazilacaksa StandartDownloadEngine , TimeBasedDownloadEngine ,AbsDownloadEngine class lari kesinlikle
 extend edilmez. Bu classlar ayar classlaridir. 

 Extend edilecek classlar ;
 DefaultTimeBasedEngine = Eger tarih referans alinacaksa bu class extend edilir.
 DefaultStandartEngine  = Eger filehousekeep referans alinacaksa bu class extend edilir
 */
public abstract class AbsDownloadEngine implements Runnable {

    //Upload edilecek sistem icin indirilen toplam file size
    private static long totalDownloadSize;
    //Indirilen filelari db islemlerini yapan session
    private Session ses;
    //db islerini yapan transcation
    private Transaction transac;
    //Sistem ait her ip bu class olusturulur ve thread baslatilir.
    public final ServerIpList eachIp;

    private final ConnectionInfo info;
    //Remote connection obj
    private Connection conObj;

    private int commitCounter = 1000;

    private int currentDownloadTime = 0;

    private boolean currentProgress = true;

    public AbsDownloadEngine(ServerIpList eachIp) {
        this.eachIp = eachIp;
        this.info = new ConnectionInfo(eachIp.getIp(), eachIp.getUname(), eachIp.getUpass(), eachIp.getPort() == null ? 0 : eachIp.getPort());
    }

    public static synchronized long getTotalDownloadSize() {
        return totalDownloadSize;
    }

    public boolean isCurrentProgress() {
        return currentProgress;
    }

    protected ServerIpList getServerInfo() {
        return eachIp;
    }

    protected Connection getRemoteConnection() {
        return conObj;
    }

    protected void setRemoteConnectionConnection() {
        this.conObj = ConnectionFactory.getInstance().createConnection(this.info);
    }

    protected ConnectionInfo getConnectionInfoObject() {
        return this.info;
    }

    /*
     Connection lib her sistem icin ayri olarak ayarlanir
     */
    abstract void setConnectionInfoLib(ConnectionInfo info);

    /*
     Ip icin acilan thread sonuna gelindiginde yapilacak islemler bu metod da yapilir.
     */
    abstract void afterFinishForCurrentThread(Connection con);

    /*
     Default commit size 1000 dir. Db de ki degisimi aninda gormek istenirse bu rakam buradan set edilebilir.
     */
    protected void setCommitSize(int size) {
        this.commitCounter = size;
    }

    void examineRemoteFile(FileObject fileObj) {
    }

    void examineLocalFile(File file) {
    }

    /*
     File housekeep den cekilen objectler icin bu metod kullanilir. Download status a gore downloaded kolonunu 1 yapar 
     yada download_try_count kolonunu bir artirir.StandartEngine sistemlerde fileCreated date set etmek için kullanılır.
     */
    public void dbOperationExistFileObject(FileHousekeep fileObj, boolean downloadStatus, String fullRemotePath) {
        checkBeforeSaveorUpdateObject();

        if (downloadStatus) {
            fileObj.setDownloaded(1);
            fileObj.setFileDownloadDate(new Date());
            if (conObj == null) { // for CopyEngine
                File file = new File(fullRemotePath);
                fileObj.setFileCreatedDate(new Date(file.lastModified()));
                fileObj.setFileSize(file.length());
                updateTotalDownloadSize(fileObj.getFileSize());
            } else {
                try {
                    RemoteFileObject remoteFileInfo = conObj.getRemoteFileInfo(fullRemotePath);
                    fileObj.setFileCreatedDate(remoteFileInfo.getDate());
                    fileObj.setFileSize(remoteFileInfo.getFileSize());
                    updateTotalDownloadSize(fileObj.getFileSize());
                } catch (Exception e) {
                }
            }
        }
        fileObj.setDownloadTryCount(fileObj.getDownloadTryCount() + 1);
        if (fileObj.getFileDate() == null || fileObj.getOperatorName() == null || fileObj.getSystemType() == null) {
            System.out.println(" Null housekeepObj for file: " + fileObj.getFileName() + ". ConnectionId: " + fileObj.getConnectionId());
        } else {
            ses.update(fileObj);
        }
        currentDownloadTime++;
    }

    /*
     Ftpden okunup download edilen filelar housekeep e insert icin bu metodu kullanilir.
     TimeBased sistemler kullanır.
     */
    public void dbOperationNonExistFileObject(FileHousekeep fileObj, FileObject remoteFileObj) {
        checkBeforeSaveorUpdateObject();
        try {
            fileObj.setFileDownloadDate(new Date());
            updateTotalDownloadSize(fileObj.getFileSize());
        } catch (NullPointerException ex) {
        }
        if (remoteFileObj != null) {
            examineRemoteFile(remoteFileObj);
        }
        if (fileObj.getFileDate() == null || fileObj.getOperatorName() == null || fileObj.getSystemType() == null) {
            System.out.println(" Null housekeepObj for file: " + fileObj.getFileName() + ". ConnectionId: " + fileObj.getConnectionId());
        } else {
            ses.save(fileObj);
        }
        currentDownloadTime++;
    }

    private void checkBeforeSaveorUpdateObject() {
        if (currentDownloadTime > commitCounter) {
            try {
                transac.commit();
                currentDownloadTime = 0;
                transac = null;
            } catch (Exception e) {
                transac.rollback();

            } finally {
                ses.close();
                ses = null;
            }
        }

        if (ses == null) {
            openSession();
            startTransaction();
        }
    }

    private void openSession() {
        ses = HibernateUtility.getSessionFactory().openSession();
    }

    private void startTransaction() {
        transac = ses.beginTransaction();
    }

    public void requestStop() {
        currentProgress = false;
    }

    @Override
    public void run() {
        if (transac != null) {
            try {
                transac.commit();
            } catch (Exception e) {
                e.printStackTrace();
                transac.rollback();
            } finally {
                ses.close();
            }
        }
    }

    public synchronized static void updateTotalDownloadSize(long size) {
        totalDownloadSize = totalDownloadSize + size;
    }

}
