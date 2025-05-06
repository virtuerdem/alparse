/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.factory.FileObject;
import com.ttgint.downloadEngine.connection.factory.LocalFileLib;
import com.ttgint.downloadEngine.connection.factory.LocalFileObject;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.hibernate.pojos.SystemDownloadTimes;
import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.util.Date;
import java.util.List;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 *
 * @author TurgutSimsek
 */
public abstract class CopyEngine extends AbsDownloadEngine {

    private SystemDownloadTimes dbDownloadTimeLog;
    private Date lastDateFromdb;
    private Date lastDateFromRemote;
    private LocalFileLib fileLib;

    public CopyEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    void setConnectionInfoLib(ConnectionInfo info) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    void afterFinishForCurrentThread(Connection con) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private void setFileLib(LocalFileLib fileLib) {
        this.fileLib = fileLib;
    }

    private void lastTimeFromDb() {

        Session ses = HibernateUtility.getSessionFactory().openSession();
        Query que = ses.getNamedQuery("getSystemMaxDownloadTimeWithoutIp");
        que.setParameter("systemId", DownloadApp.currentProgressObject.getSystemId());
        try {
            dbDownloadTimeLog = (SystemDownloadTimes) que.list().get(0);
        } catch (java.lang.IndexOutOfBoundsException e) {
            System.out.println("Creating new date");
            dbDownloadTimeLog = new SystemDownloadTimes();
            dbDownloadTimeLog.setIpId(getServerInfo().getConnectionId().toString());
            dbDownloadTimeLog.setLastDownloadDate(new Date());
            dbDownloadTimeLog.setSystemId(DownloadApp.currentProgressObject.getSystemId());
        }
        lastDateFromdb = dbDownloadTimeLog.getLastDownloadDate();
        lastDateFromRemote = dbDownloadTimeLog.getLastDownloadDate();
        ses.close();
    }

    public void updateLastTime() {
        if (lastDateFromRemote.after(lastDateFromdb)) {
            Session ses = HibernateUtility.getSessionFactory().openSession();
            Transaction trx = ses.beginTransaction();
            try {
                ses.saveOrUpdate(dbDownloadTimeLog);
                trx.commit();
            } catch (Exception e) {
                //e.printStackTrace();
                trx.rollback();
            } finally {
                ses.close();
            }
        } else {
            System.out.println(eachIp.getIp() + ":" + eachIp.getRemotePath() + " Nothing downloaded , date updating is not neccesary!");
        }
    }

    public Date getLastDateFromdb() {
        return lastDateFromdb;
    }

    /*
    remote file dan cekilen her file getLastDateFromdb metoduyla gelen field la karsilastirilir.Eger remote file date bu
    tarihten buyuk ise dosya indirilir. Indirilen remote file bu metoda gonderilir. Bunun yapilmasindaki amac 
    olusan en son file bulup db deki date i guncellemektir. Herahngi bir degisiklige gerek yoktur indirilen remotefile i 
    bu metoda gondermek yeterlidir.
     */
    @Override
    public void examineRemoteFile(FileObject fileObj) {
        if (lastDateFromRemote == null) {
            lastDateFromRemote = fileObj.getDate();
            dbDownloadTimeLog.setLastDownloadDate(fileObj.getDate());
            super.examineRemoteFile(fileObj);
            return;
        }
        if (fileObj.getDate().after(lastDateFromRemote)) {
            lastDateFromRemote = fileObj.getDate();
            dbDownloadTimeLog.setLastDownloadDate(fileObj.getDate());
        }
        super.examineRemoteFile(fileObj);
    }

    public abstract void copyFiles(Date lastDateFromDb, LocalFileLib fileLib, List<LocalFileObject> files, ServerIpList connectionInfo);

    @Override
    public void run() {
        lastTimeFromDb();
        setFileLib(new LocalFileLib());
        copyFiles(getLastDateFromdb(), fileLib, fileLib.readAllFilesInCurrentPath(getServerInfo().getRemotePath()), getServerInfo());
        updateLastTime();
        super.run();
    }

}
