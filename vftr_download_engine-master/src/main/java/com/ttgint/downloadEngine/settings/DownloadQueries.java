/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.settings;

import com.ttgint.downloadEngine.main.DownloadApp;

/**
 *
 * @author TTGParserTeam
 */
public class DownloadQueries {

    private DownloadQueries() {
    }

    public static String downloadQueryForHuaweiCm(String systemType, String operatorName, String date, String ip) {
        String tableName = null;
        switch (operatorName) {
            case "VODAFONE":
                switch (systemType) {
                    case "HW5G":
                        tableName = "NORTHI_PARSER.OBJECTS_HW5G";
                        break;
                    case "HW4G":
                        tableName = "NORTHI_PARSER.OBJECTS_HW4G";
                        break;
                    case "HW3G":
                        tableName = "M2000.OBJECTS_HW3G";
                        break;
                    case "HW2G":
                        tableName = "NORTHI_PARSER.OBJECTS_HW2G";
                        break;
                }
                break;
            case "KKTC-TELSIM":
                switch (systemType) {
                    case "HW4G":
                        tableName = "NORTHI_PARSER_KKTC.OBJECTS_HW4G";
                        break;
                    case "HW3G":
                        tableName = "NORTHI_PARSER_KKTC.OBJECTS_HW3G";
                        break;
                    case "HW2G":
                        tableName = "NORTHI_PARSER_KKTC.OBJECTS_HW2G";
                        break;
                }
                break;
        }

        String query;
        if (systemType.equals("HW4G") || systemType.equals("HW5G")) {
            query = " select ne_name from NORTHI_PARSER_SETTINGS.PARSER_USED_NES "
                    + " where system_type = '" + systemType + "' "
                    + " AND M2000_SERVER_IP_CM = '" + ip + "' "
                    + " AND IS_ACTIVE = 1 "
                    + " AND OPERATOR_NAME = '" + operatorName + "' "
                    + " minus  "
                    + " select A.ne_name from " + tableName + " A,NORTHI_PARSER_SETTINGS.PARSER_USED_NES B "
                    + " where A.ne_type = '30' "
                    + " and A.data_date=to_date('" + date + "','yyyyMMdd') "
                    + " AND B.SYSTEM_TYPE = '" + systemType + "' "
                    + " AND A.NE_NAME = B.NE_NAME "
                    + " AND B.M2000_SERVER_IP_CM = '" + ip + "' ";
        } else {
            query = " select ne_name from NORTHI_PARSER_SETTINGS.PARSER_USED_NES "
                    + " where system_type = '" + systemType + "' "
                    + " AND M2000_SERVER_IP_CM = '" + ip + "' "
                    + " AND IS_ACTIVE = 1 "
                    + " AND OPERATOR_NAME = '" + operatorName + "' "
                    + " minus  "
                    + " select A.ne_name from " + tableName + " A,NORTHI_PARSER_SETTINGS.PARSER_USED_NES B "
                    + " where A.ne_type = '31' "
                    + " and A.data_date=to_date('" + date + "','yyyyMMdd') "
                    + " AND B.SYSTEM_TYPE = '" + systemType + "' "
                    + " AND A.NE_NAME = B.NE_NAME "
                    + " AND B.M2000_SERVER_IP_CM = '" + ip + "' ";
        }
        return query;

    }

    public static String getOmcAndTableNameQuery(String omcIp) {
        String query = "SELECT DISTINCT TABLE_NAME,NE_NAME FROM ( select case  when  table_name like '%CELL_STATISTIC%' THEN 'CELL_STATISTIC' ELSE "
                + "TABLE_NAME END AS TABLE_NAME,NE_NAME from parser_Raw_Table_list A , PARSER_USED_NES B where A.system_Type ='MOTOROLA'  "
                + "AND B.SYSTEM_TYPE = A.SYSTEM_TYPE AND B.IS_aCTIVE = 1 "
                + " AND A.TABLE_NAME <> 'ENTITY' AND B.M2000_SERVER_IP ='" + omcIp + "' ) A WHERE A.TABLE_NAME IS NOT NULL";
        return query;
    }

    public static String getActiveNeNames() {
        String query = String.format("SELECT NE_NAME,NE_VERSION FROM PARSER_USED_NES WHERE IS_ACTIVE=1 AND SYSTEM_TYPE='%s'", DownloadApp.SYSTEMTYPE);
        return query;
    }

    public static String getFunctionSubsetIdAndTableNameFromParserRawTableList(String operatorName, String measType, String systemType) {
        String query = String.format("SELECT FUNCTIONSUBSET_ID, TABLE_NAME FROM PARSER_RAW_TABLE_LIST WHERE OPERATOR_NAME='%s' AND MEAS_TYPE='%s' AND"
                + " SYSTEM_TYPE = '%s' AND IS_ACTIVE = 1", operatorName, measType, systemType);

        return query;
    }

    public static String getFunctionSubsetNameAndTableNameFromParserRawTableList(String operatorName, String measType, String systemType) {
        String query = String.format("SELECT FUNCTIONSUBSETNAME, TABLE_NAME FROM PARSER_RAW_TABLE_LIST WHERE OPERATOR_NAME='%s' AND MEAS_TYPE='%s' AND"
                + " SYSTEM_TYPE = '%s'", operatorName, measType, systemType);

        return query;
    }

    public static String getFunctionSubsetNameAndTableNameFromParserRawTableListActive(String operatorName, String measType, String systemType) {
        String query = String.format("SELECT FUNCTIONSUBSETNAME, TABLE_NAME FROM PARSER_RAW_TABLE_LIST WHERE OPERATOR_NAME='%s' AND MEAS_TYPE='%s' AND"
                + " SYSTEM_TYPE = '%s' AND IS_ACTIVE = 1", operatorName, measType, systemType);

        return query;
    }

    public static String getParserRawTableListNeTypes(String operatorName, String measType, String systemType) {
        String query = String.format("SELECT DISTINCT NE_TYPE FROM PARSER_RAW_TABLE_LIST WHERE NE_TYPE IS NOT NULL AND IS_ACTIVE = 1 "
                + " AND OPERATOR_NAME='%s' AND MEAS_TYPE='%s' AND SYSTEM_TYPE = '%s'", operatorName, measType, systemType);
        return query;
    }

    public static String getParserCounterList(String operatorName, String measType, String systemType) {
        String query = String.format("SELECT TABLE_NAME,COUNTER_NAME_FILE FROM PARSER_COUNTER_LIST WHERE OPERATOR_NAME='%s' AND MEAS_TYPE='%s' AND"
                + " SYSTEM_TYPE = '%s' AND IS_ACTIVE = 1 AND COUNTER_MODEL = 'VARIABLE' ORDER BY TABLE_NAME,COUNTER_ID", operatorName, measType, systemType);

        return query;
    }

    public static String insertErrorLog(String operatorName, String systemType, String measType, String errorDetail, String mailStatus, String smsStatus) {
        String query = String.format("INSERT INTO ERROR_LOG (OPERATOR_NAME,SYSTEM_TYPE,MEAS_TYPE,ERROR_TIME,ERROR_DETAIL,MAIL_STATUS,SMS_STATUS)"
                + " VALUES ('%s','%s','%s',SYSDATE,'%s','%s','%s')", operatorName, systemType, measType, errorDetail, mailStatus, smsStatus);

        return query;
    }

    public static String getFunctionSubsetNames(String schema, String operatorName, String systemType, String measType) {
        String query = String.format(
                "SELECT FUNCTIONSUBSETNAME "
                + "FROM %s.PARSER_RAW_TABLE_LIST "
                + "WHERE OPERATOR_NAME = '%s' "
                + "AND SYSTEM_TYPE = '%s' "
                + "AND MEAS_TYPE = '%s' "
                + "AND IS_ACTIVE = 1 ",
                schema, operatorName, systemType, measType);

        return query;
    }

    public static String getGsmaTask() {
        String query = String.format("SELECT GSMA_FILE_UPLOAD_ID, FILE_NAME\n"
                + " FROM MCKS.GSMA_FILE_UPLOAD\n"
                + " WHERE IS_PROCESS = 0\n"
                + " ORDER BY PROCESS_REQUEST_DATE");
        return query;
    }
}
