/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ttgint.downloadEngine.fileoperations;

import com.ttgint.downloadEngine.annatotians.DownloadEngine;
import com.ttgint.downloadEngine.common.CommonLibrary;
import com.ttgint.downloadEngine.engines.SnmpEngine;
import com.ttgint.downloadEngine.hibernate.pojos.ServerIpList;
import com.ttgint.downloadEngine.main.DownloadApp;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.snmp4j.Snmp;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.DefaultPDUFactory;
import org.snmp4j.util.TreeEvent;
import org.snmp4j.util.TreeUtils;

/**
 *
 * @author erdigurbuz
 */
@DownloadEngine(systemType = "NAS", measType = "PM", operatorName = "VODAFONE")
public class NASDownloadEngine extends SnmpEngine {

    public NASDownloadEngine(ServerIpList eachIp) {
        super(eachIp);
    }

    @Override
    public void request(HashMap<String, String> objectTableMap) {

        Snmp snmp = new Snmp(transport);

        for (Object obj : objectTableMap.keySet()) {
            // Create Snmp object for sending data to Agent
            OID oid = new OID(obj.toString());

            TreeUtils treeUtils = new TreeUtils(snmp, new DefaultPDUFactory());
            List<TreeEvent> events = treeUtils.getSubtree(comtarget, oid);
            if (events == null || events.size() == 0) {
            }

            // Get snmpwalk result.
            HashMap<Integer, String> dataMap = new HashMap<>();
            for (TreeEvent event : events) {
                if (event.isError()) {
                    System.out.println("OID has an error: " + event.getErrorMessage());
                } else {
                    VariableBinding[] varBindings = event.getVariableBindings();
                    if (varBindings == null || varBindings.length == 0) {
                        //System.out.println("VarBinding: No result returned.");
                    }
                    for (VariableBinding varBinding : varBindings) {
                        varBinding.toValueString();
                        int key = Integer.parseInt(varBinding.getOid().toString().split("\\.")[varBinding.getOid().toString().split("\\.").length - 1]);
                        if (dataMap.get(key) == null) {
                            dataMap.put(key, varBinding.getVariable().toString());
                        } else {
                            dataMap.put(key, dataMap.get(key) + "|" + varBinding.getVariable().toString());
                        }
                    }
                }
            }

            try {
                FileOutputStream fos = null;
                fos = new FileOutputStream(DownloadApp.LOCALFILEPATH + new File(eachIp.getIp() + "+" + objectTableMap.get(obj) + ".unl"), true);
                for (Integer k : dataMap.keySet()) {
                    fos.write((CommonLibrary.get_CurrentDatetime("yyyyMMddHH") + "|" + dataMap.get(k) + "\n").getBytes());
                }
                fos.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }
        try {
            snmp.close();
        } catch (IOException ex) {
        }
    }

}
