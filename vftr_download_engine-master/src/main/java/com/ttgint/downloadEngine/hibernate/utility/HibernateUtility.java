/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.hibernate.utility;

import static com.ttgint.downloadEngine.main.DownloadApp.hibernateConfigFileName;
import java.io.File;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;

/**
 * Hibernate Utility class with a convenient method to get Session Factory object.
 *
 * @author EnesTerzi
 */
public class HibernateUtility {

    private static SessionFactory sessionFactory;

    static {
        rebuildSessionFactroy();
    }

    public static void rebuildSessionFactroy() {
        if (sessionFactory instanceof SessionFactory) {
            closeSessionFactoryIfC3P0ConnectionProvider(sessionFactory);
        }
        Configuration configuration = new Configuration();
        configuration.configure(new File(hibernateConfigFileName));
        StandardServiceRegistryBuilder ssrb
                = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties());
        sessionFactory = configuration.buildSessionFactory(ssrb.build());
    }

    public synchronized static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static boolean closeSessionFactoryIfC3P0ConnectionProvider(SessionFactory factory) {
        try {
            sessionFactory.close();
        } catch (Exception ex) {
            return false;
        }
        return true;
    }
}
