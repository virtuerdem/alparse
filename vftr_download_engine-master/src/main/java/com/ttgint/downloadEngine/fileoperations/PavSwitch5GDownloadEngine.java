/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.engines.SnmpEngine;
import com.ttgint.downloadEngine.hibernate.pojos.FileHousekeep;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.codehaus.plexus.util.StringUtils;
import org.snmp4j.PDU;
import org.snmp4j.ScopedPDU;
import org.snmp4j.Snmp;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "PAVSWITCH", measType = "PM", operatorName = "HTK")
public class PavSwitch5GDownloadEngine extends SnmpEngine {

    public String ip;
    public List<String> listNeName;
    private int rowCount;

    public PavSwitch5GDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
        this.ip = eachIp.getIp();
    }

    @Override
    public void request(HashMap<String, String> objectTableMap) {
        Snmp snmp = new Snmp(transport);

        for (Object obj : objectTableMap.keySet()) {
            rowCount = 0;
            List<String> counterList = objectTableCounterMap.get(objectTableMap.get(obj));
            String responseOID = "";
            for (String list : counterList) {
                if (list.startsWith(obj.toString())) {
                    try {
                        PDU pdu = null;
                        if (comtarget.getVersion() == SnmpConstants.version3) {
                            pdu = new ScopedPDU();
                        } else {
                            pdu = new PDU();
                        }
                        pdu.setType(PDU.GET);
                        pdu.add(new VariableBinding(new OID(list)));
                        ResponseEvent responseEvent = snmp.send(pdu, comtarget);
                        PDU response = responseEvent.getResponse();
                        responseOID += "," + response.get(0).getVariable().toString();
                    } catch (Exception ex) {
                        responseOID += ",invalidOID";
                    }
                } else {
                    responseOID += ",unmatchedOID";
                }
            }

            if (counterList.size() > 0) {
                if (StringUtils.countMatches(responseOID, "noSuchObject") + StringUtils.countMatches(responseOID, "unmatchedOID") + 1
                        < responseOID.chars().filter(ch -> ch == ',').count()) {
                    try {
                        FileOutputStream fos = null;
                        fos = new FileOutputStream(DownloadApp.LOCALFILEPATH + new File(ip + "+" + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd-HH:mm") + "+" + objectTableMap.get(obj) + ".txt"), true);
                        fos.write((CommonLibrary.get_CurrentDatetime("yyyy-MM-dd HH:mm") + "," + ip + responseOID + "\n").getBytes());
                        fos.close();
                        rowCount++;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                } else {

                }
            }
            if (rowCount > 0) {
                FileHousekeep fileHousekeepObj = new FileHousekeep();
                fileHousekeepObj.setDownloaded(1);
                fileHousekeepObj.setDownloadTryCount(1);
                fileHousekeepObj.setFileDate(new Date());
                fileHousekeepObj.setFileName(ip + "+" + CommonLibrary.get_CurrentDatetime("yyyy-MM-dd-HH:mm") + "+" + objectTableMap.get(obj) + ".txt");
                fileHousekeepObj.setFileSize(1L);
                fileHousekeepObj.setConnectionId(eachIp.getConnectionId());
                fileHousekeepObj.setOperatorName(DownloadApp.OPERATORNAME);
                fileHousekeepObj.setSystemType(DownloadApp.SYSTEMTYPE);
                fileHousekeepObj.setMeasType(DownloadApp.MEASTYPE);
                fileHousekeepObj.setFileCreatedDate(new Date());

                dbOperationNonExistFileObject(fileHousekeepObj, null);
            }
        }

        try {
            snmp.close();
        } catch (IOException ex) {
        }
    }

}
