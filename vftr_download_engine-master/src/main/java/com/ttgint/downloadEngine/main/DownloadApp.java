/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.main;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.annatotians.DownloadEngines;
import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.engines.AbsDownloadEngine;
import com.ttgint.downloadEngine.hibernate.pojos.DownloadProcessLog;
import com.ttgint.downloadEngine.hibernate.pojos.ParserStartStop;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import com.ttgint.downloadEngine.settings.DownloadEngager;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.internal.SessionFactoryImpl;

/**
 *
 * @author EnesTerzi, erdigurbuz
 */
public class DownloadApp {

    public static long myPidNo;

    public static final String mainClassName = "com.ttgint.downloadEngine.main.DownloadApp";

    public static final String configFileName = "downloadEngine.properties";

    public static final String hibernateConfigFileName = "downloadEngineHibernate.cfg.xml";

    public static final String hibernateLogger = "org.hibernate";

    public static final String RUNNINGMESSAGE = "RUNNING";

    public static final String STOPPEDMESSAGE = "STOPPED";

    public static final String DEACTIVATEDMESSAGE = "DEACTIVATED";

    public static final String FORCEMESSAGE = "FORCE";

    public static final String COMPLETEDMESSAGE = "COMPLETED";

    public static final String CLASSNOTFOUNDERROR = "CLASSNOTFOUND";

    public static String DEFAULT_CLOSE_MESSAGE = "NOT FINISHED";

    public static String ERROR_MESSAGE = "FAILED";

    public static String CLOSEMESSAGE;

    public static boolean hibernatedebug;

    public static String HIBERNATESHOWSQLFLAG;

    public static String HIBERNATESHOWSQLFORMATFLAG;

    public static String TNSPATH;

    public static String LOCALFILEPATH;

    public static String SYSTEMTYPE;

    public static String EDITTEDSYSTEMTYPE;

    public static String MEASTYPE;

    public static String OPERATORNAME;

    public static String OPERATORSHORTCODE;

    public static String FILESYSTEM;

    public static int threadCount;

    public static List<ServerIpList> currentSystemIpList;

    public static DownloadProcessLog currentProgressLog;

    public static ParserStartStop currentProgressObject;

    public static boolean dbConnectionStatus = false;

    public static String DBType;

    public static void main(String[] args) throws IOException {
        if (args.length == 4) {
            if (args[3].equals("--ml") || args[3].equals("--ML")) {
                DownloadProperties.getInstance().setLogforManagerflag(true);
            }
        }
        System.out.println("Download Started at : " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm"));
        //Logger LOG =  Logger.getLogger(hibernateLogger);
        myPidNo = CommonLibrary.getPid();

        DownloadLogger.printLogForManager(String.valueOf(myPidNo), DownloadLogger.ManagerLogLevel.PID);

        sleepLitte();

        parserArgs(args);

        OPERATORNAME = args[0];

        SYSTEMTYPE = args[1];

        EDITTEDSYSTEMTYPE = SYSTEMTYPE.split("\\-")[0];

        MEASTYPE = args[2];

        CLOSEMESSAGE = DEFAULT_CLOSE_MESSAGE;

        setProperties();
        dbConnectionStatus = getDbConnectionStatus();
        DownloadLogger.printLogForManager("Connecting Db", DownloadLogger.ManagerLogLevel.STATE);
        // checking db connection
        if (dbConnectionStatus) {
            //getting parserstart stop object specific system
            Session ses = HibernateUtility.getSessionFactory().openSession();
            currentProgressObject = (ParserStartStop) ses.getNamedQuery("findByOther")
                    .setParameter("systemType", SYSTEMTYPE).setParameter("measType", MEASTYPE)
                    .setParameter("operatorName", OPERATORNAME).list().get(0);

            OPERATORSHORTCODE = currentProgressObject.getOperatorShortCode();
            LOCALFILEPATH = DownloadApp.LOCALFILEPATH + DownloadApp.OPERATORSHORTCODE + "-" + DownloadApp.SYSTEMTYPE + DownloadApp.MEASTYPE + "/";
            threadCount = currentProgressObject.getParserThread();

            if (currentProgressObject.getDownloadStatus().equals(RUNNINGMESSAGE)) {
                System.out.println("Download is working");
                return;
            }

            if (currentProgressObject.getStatus().equals(RUNNINGMESSAGE)) {
                System.out.println("Parser is working");
                return;
            }
            Transaction trx = ses.beginTransaction();
            currentProgressObject.setDownloadStatus(RUNNINGMESSAGE);
            ses.update(currentProgressObject);
            trx.commit();

            ses.close();
            insertLog();

            try {
                if (currentProgressObject.getIsDownloadActive()) {
                    CLOSEMESSAGE = COMPLETEDMESSAGE;
                    switch (SYSTEMTYPE) {
                        case "MOTOROLA-HOURLY":
                        case "MOTOROLA-DAILY":
                            currentSystemIpList = getActiveIpListForCurrentSystem(EDITTEDSYSTEMTYPE);
                            break;

                        default:
                            currentSystemIpList = getActiveIpListForCurrentSystem(SYSTEMTYPE);
                            break;
                    }

                    Class classObject = findClass();
                    if (classObject != null) {
                        DownloadEngager engager = new DownloadEngager(classObject);
                        DownloadLogger.printLogForManager("Downloading", DownloadLogger.ManagerLogLevel.STATE);
                        engager.engageDownload();
                    } else {
                        CLOSEMESSAGE = CLASSNOTFOUNDERROR;
                    }

                } else {
                    CLOSEMESSAGE = DEACTIVATEDMESSAGE;
                }
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | IllegalArgumentException | InvocationTargetException e) {

                CLOSEMESSAGE = ERROR_MESSAGE;

            } catch (ClassNotFoundException ex) {
                CLOSEMESSAGE = CLASSNOTFOUNDERROR;
            } finally {

            }

        } else {
            System.out.println("Database Connection Error");
        }
        closeLog();
        HibernateUtility.closeSessionFactoryIfC3P0ConnectionProvider(HibernateUtility.getSessionFactory());
        System.out.println("Close Message : " + CLOSEMESSAGE);
        System.out.println("Download Finished at : " + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm"));
    }

    private static void setProperties() throws FileNotFoundException, IOException {
        System.out.println("Setting Properties");
        Properties localprop = new Properties();
        localprop.load(new FileInputStream(new File(configFileName)));
        TNSPATH = localprop.getProperty("TNSPATH");
        LOCALFILEPATH = localprop.getProperty("LOCALFILEPATH");
        HIBERNATESHOWSQLFLAG = localprop.getProperty("HIBERNATESHOWSQLFLAG");
        HIBERNATESHOWSQLFORMATFLAG = localprop.getProperty("HIBERNATESHOWSQLFORMATFLAG");
        hibernatedebug = Boolean.parseBoolean(localprop.getProperty("DBDEBUG"));
        Properties p = new Properties(System.getProperties());
        p.put("oracle.net.tns_admin", TNSPATH);
        p.put("hibernate.show_sql", HIBERNATESHOWSQLFLAG);
        p.put("hibernate.format_sql", HIBERNATESHOWSQLFORMATFLAG);
        System.setProperties(p);
    }

    private static void sleepLitte() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {

        }
    }

    private static List<ServerIpList> getActiveIpListForCurrentSystem(String systemType) {
        Session ses = HibernateUtility.getSessionFactory().openSession();
        Query que = ses.getNamedQuery("getIpList");
        que.setParameter("measType", MEASTYPE);
        que.setParameter("systemType", systemType);
        que.setParameter("operatorName", OPERATORNAME);
        List<ServerIpList> list = new ArrayList<>(que.list());
        ses.close();
        return list;
    }

    private static void insertLog() {
        currentProgressLog = new DownloadProcessLog();
        currentProgressLog.setMeasType(MEASTYPE);
        currentProgressLog.setOperatorName(OPERATORNAME);
        currentProgressLog.setProcessStartTime(new Date());
        currentProgressLog.setSystemType(SYSTEMTYPE);
        currentProgressLog.setPidNo(myPidNo);
        currentProgressLog.setProgressStatus(RUNNINGMESSAGE);
        Session ses = HibernateUtility.getSessionFactory().openSession();
        try {
            Transaction trx = ses.beginTransaction();
            ses.save(currentProgressLog);
            trx.commit();
        } catch (Exception e) {

        } finally {
            ses.close();
        }
    }

    private static void closeLog() {
        System.out.println("Closing logs");
        Session ses = HibernateUtility.getSessionFactory().openSession();
        Transaction trx = ses.beginTransaction();
        try {
            //Tekrar aliniyor flag'lerde download sirasinda degisiklik olabildigi icin
            currentProgressObject = (ParserStartStop) ses.getNamedQuery("findByOther")
                    .setParameter("systemType", SYSTEMTYPE).setParameter("measType", MEASTYPE)
                    .setParameter("operatorName", OPERATORNAME).list().get(0);
            currentProgressObject.setDownloadStatus(STOPPEDMESSAGE);
            currentProgressLog.setProgressStatus(CLOSEMESSAGE);
            currentProgressLog.setProcessStopTime(new Date());
            currentProgressLog.setTotalDownloadSize(AbsDownloadEngine.getTotalDownloadSize());
            ses.update(currentProgressObject);
            ses.update(currentProgressLog);
            trx.commit();
        } catch (Exception e) {
            e.printStackTrace();
            trx.rollback();
        } finally {
            ses.close();
        }
    }

    public static boolean getDbConnectionStatus() {
        boolean status;
        try {
            Session ses = HibernateUtility.getSessionFactory().openSession();
            status = true;
            DBType = ((SessionFactoryImpl) HibernateUtility.getSessionFactory()).getProperties().get("hibernate.connection.url").toString().split("\\:")[1].toUpperCase();
            ses.close();
        } catch (Exception e) {
            status = false;
        }
        return status;
    }

    public static void parserArgs(String[] args) {

    }

    private static Class checkforMulti(Class classObject) {
        DownloadEngines system = CommonLibrary.AnnatStaticOperations.getDownlaodEnginesAnat(classObject);
        for (DownloadEngine each : system.downloadEngines()) {
            if (checkSystemIsTrue(each)) {
                return classObject;
            }
        }
        return null;
    }

    private static Class checkforSingle(Class classObject) {
        DownloadEngine system = CommonLibrary.AnnatStaticOperations.getDownlaodEngineAnat(classObject);
        if (checkSystemIsTrue(system)) {
            return classObject;
        }
        return null;
    }

    private static boolean checkSystemIsTrue(DownloadEngine system) {
        return system.measType().equals(MEASTYPE)
                && system.operatorName().equals(OPERATORNAME)
                && system.systemType().equals(SYSTEMTYPE);
    }

    private static File getcurrentRunningJarFile() throws ClassNotFoundException {
        Class clazz = null;
        clazz = Class.forName(mainClassName);
        if (clazz != null && clazz.getProtectionDomain() != null
                && clazz.getProtectionDomain().getCodeSource() != null) {
            URL codeLocation = clazz.getProtectionDomain().getCodeSource()
                    .getLocation();

            String name = codeLocation.toString();
            name = name.replace("file:", "");
            //Open For Local TEST
//            if (new File(name).isDirectory() && name.contains("classes")) {
//                name = name.split("classes")[0] + "DownloadEngine.jar";
//            }
            return new File(name);
        }
        return null;
    }

    public static Class findClass() throws ClassNotFoundException {
        File jarFile = getcurrentRunningJarFile();
        if (!(jarFile instanceof File)) {
            System.out.println("Cannot Find File ");
            System.exit(1);
        }
        JarFile jarFileInputStream = null;
        try {
            jarFileInputStream = new JarFile(jarFile);
        } catch (IOException ex) {
            System.out.println(ex.toString());
            System.out.println("Cannot read jar File Corrupted!");
            System.exit(1);
        }
        Enumeration<JarEntry> jarList = jarFileInputStream.entries();

        Class classObject = null;

        while (jarList.hasMoreElements()) {
            String className = (jarList.nextElement().getName());
            if (!className.endsWith("class")) {
                continue;
            }
            try {
                Class<?> clas = Class.forName(className.replace(".class", "").replace("/", "."));
                //       System.out.println(clas.getName());
                if (CommonLibrary.AnnatStaticOperations.isDownloadEngines(clas)) {
                    classObject = checkforMulti(clas);
                } else if (CommonLibrary.AnnatStaticOperations.isDownloadEngine(clas)) {
                    classObject = checkforSingle(clas);
                } else {

                }
                if (classObject != null) {
                    break;
                }

            } catch (ClassNotFoundException ex) {
                System.out.println(ex.toString());
            }
        }
        if (classObject != null) {
            return classObject;
        } else {
            throw new RuntimeException("Class Not Found");
        }
    }

}
