/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.connection.factory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author TTGETERZI
 */
public class RemoteFileUtility {

    public static List<RemoteFileObject> filterListByRegex(String pattern, List<RemoteFileObject> list) {
        List<RemoteFileObject> newList = new ArrayList<>();
        Pattern pat = Pattern.compile(pattern);
        for (RemoteFileObject each : list) {
            Matcher mat = pat.matcher(each.getFileName());
            if (mat.find()) {
                newList.add(each);
            }
        }
        return newList;

    }

    public static List<RemoteFileObject> filterListAfterDate(Date afterDate, List<RemoteFileObject> list) {
        List<RemoteFileObject> newList = new ArrayList<>();
        for (RemoteFileObject each : list) {
            if (each.getDate().after(afterDate)) {
                newList.add(each);
            }
        }
        return newList;
    }

}
