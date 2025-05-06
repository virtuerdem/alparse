/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.common;

import com.ttgint.downloadEngine.hibernate.utility.HibernateUtility;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.transform.Transformers;

/**
 *
 * @author TTGETERZI
 */
public class DaoUtils {

    public static <X> List<X> getObject(Class<X> classObject, String queryName,
            String[] parametres, Object[] paramatresValues) {
        if ((parametres != null && paramatresValues != null) && (parametres.length != paramatresValues.length)) {
            return null;
        }
        Session ses = HibernateUtility.getSessionFactory().openSession();
        Query que = ses.getNamedQuery(queryName);

        if (parametres instanceof Object && parametres.length > 0) {
            for (int i = 0; i < parametres.length; i++) {
                que.setParameter(parametres[i], paramatresValues[i]);
            }
        }

        List<X> list = que.list();
        ses.close();
        return new ArrayList<>(list);
    }

    public static boolean saveObject(Object object) {
        Session ses;
        ses = HibernateUtility.getSessionFactory().openSession();
        Transaction trx = ses.beginTransaction();
        try {
            ses.save(object);
            trx.commit();
            return true;
        } catch (Exception e) {
            trx.rollback();
            return false;
        } finally {
            ses.close();
        }

    }

    public static boolean updateObject(Object obj) {
        Session ses;
        ses = HibernateUtility.getSessionFactory().openSession();
        Transaction trx = ses.beginTransaction();
        try {
            ses.update(obj);
            trx.commit();
            return true;
        } catch (Exception e) {
            trx.rollback();
            return false;
        } finally {
            ses.close();
        }
    }

    public static List<Map> getQueryAsListMap(String que) {
        Session ses = HibernateUtility.getSessionFactory().openSession();
        SQLQuery query = ses.createSQLQuery(que);
        query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

        List list = query.list();
        List<Map> newList = new ArrayList<>();

        for (Object each : list) {
            Map mapObject = (Map) each;
            newList.add(mapObject);
        }
        ses.close();
        return newList;
    }

    public static void executeQuery(String que) {

        Session session = HibernateUtility.getSessionFactory().openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            SQLQuery query = session.createSQLQuery(que);
            query.executeUpdate();
            tx.commit();
        } catch (HibernateException e) {
            //System.out.println("HibernateException : " + e.getMessage());
            if (tx != null) {
                tx.rollback();
            }
        } finally {
            session.close();
        }
    }
}
