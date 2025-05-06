/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.engines;

import com.ttgint.downloadEngine.common.DaoUtils;
import com.ttgint.downloadEngine.connection.factory.Connection;
import com.ttgint.downloadEngine.connection.settings.ConnectionInfo;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import com.ttgint.downloadEngine.settings.DownloadQueries;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.snmp4j.CommunityTarget;
import org.snmp4j.TransportMapping;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 *
 * @author erdi_gurbuz
 */
public abstract class SnmpEngine extends AbsDownloadEngine {

    public TransportMapping transport;
    public CommunityTarget comtarget;
    public HashMap<String, String> objectTableMap = new HashMap<>();
    public static HashMap<String, List<String>> objectTableCounterMap = new HashMap<>();

    public SnmpEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    void setConnectionInfoLib(ConnectionInfo info) {
    }

    @Override
    public void run() {
        getObjectList();
        connectSnmpService();
        request(objectTableMap);
        super.run();
    }

    public abstract void request(HashMap<String, String> objectTableMap);

    private void getObjectList() {
        List<Map> list = DaoUtils.getQueryAsListMap(DownloadQueries.getFunctionSubsetNameAndTableNameFromParserRawTableList(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        for (Map tableData : list) {
            this.objectTableMap.put((String) tableData.get("functionsubsetname"), (String) tableData.get("table_name"));
        }

        List<Map> counterSelect = DaoUtils.getQueryAsListMap(DownloadQueries.getParserCounterList(
                DownloadApp.OPERATORNAME, DownloadApp.MEASTYPE, DownloadApp.SYSTEMTYPE));
        for (Map tableData : counterSelect) {
            if (!objectTableCounterMap.containsKey(tableData.get("table_name").toString())) {
                objectTableCounterMap.put(tableData.get("table_name").toString(), new ArrayList<String>());
            }
            objectTableCounterMap.get(tableData.get("table_name").toString()).add(tableData.get("counter_name_file").toString());
        }
    }

    private void connectSnmpService() {
        int snmpVersion = SnmpConstants.version2c;
        String community = eachIp.getElementManagerName();

        try {
            transport = new DefaultUdpTransportMapping();
            transport.listen();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        comtarget = new CommunityTarget();
        comtarget.setCommunity(new OctetString(community));
        comtarget.setVersion(snmpVersion);
        comtarget.setAddress(new UdpAddress(eachIp.getIp() + "/" + eachIp.getPort().toString()));
        comtarget.setRetries(3);
        comtarget.setTimeout(5000);
    }

    @Override
    void afterFinishForCurrentThread(Connection con) {
        try {
            transport.close();
        } catch (IOException ex) {
        }
    }
}
