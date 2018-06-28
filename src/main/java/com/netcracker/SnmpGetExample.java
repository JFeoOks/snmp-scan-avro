package com.netcracker;

import com.netcracker.model.SnmpRecord;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

public class SnmpGetExample {

    private static String ipAddress = "10.229.128.90";
    private static String port = "161";
    private static String oidValue = ".1.3.6.1.2.1.1.1.0";
    private static int snmpVersion = SnmpConstants.version1;
    private static String community = "public";

    public static void main(String[] args) throws Exception {
        TransportMapping transport = new DefaultUdpTransportMapping();
        transport.listen();

        Snmp snmp = new Snmp(transport);
        CommunityTarget comTarget = communityTarget();
        PDU pdu = pdu();

        processSnmpResponce(snmp.get(pdu, comTarget));

        snmp.close();
    }

    private static CommunityTarget communityTarget() {
        CommunityTarget result = new CommunityTarget();
        result.setCommunity(new OctetString(community));
        result.setVersion(snmpVersion);
        result.setAddress(new UdpAddress(ipAddress + "/" + port));
        result.setRetries(2);
        result.setTimeout(1000);

        return result;
    }

    private static PDU pdu() {
        PDU result = new PDU();
        result.add(new VariableBinding(new OID(oidValue)));
        result.setType(PDU.GET);
        result.setRequestID(new Integer32(1));

        return result;
    }

    private static void processSnmpResponce(ResponseEvent response) throws IOException {
        if (response != null) {
            processPduResponce(response.getResponse());
        } else {
            System.out.println("Error: Agent Timeout... ");
        }
    }

    private static void processPduResponce(PDU responsePDU) throws IOException {
        if (responsePDU != null) {
            int errorStatus = responsePDU.getErrorStatus();
            int errorIndex = responsePDU.getErrorIndex();
            String errorStatusText = responsePDU.getErrorStatusText();

            if (errorStatus == PDU.noError) {
                writeToAvro(
                        createSnmpRecords(responsePDU.getVariableBindings())
                );
            } else {
                System.out.println("Error: Request Failed");
                System.out.println("Error Status = " + errorStatus);
                System.out.println("Error Index = " + errorIndex);
                System.out.println("Error Status Text = " + errorStatusText);
            }
        } else {
            System.out.println("Error: Response PDU is null");
        }
    }

    private static List<SnmpRecord> createSnmpRecords(Vector<? extends VariableBinding> records) {
        return records
                .stream()
                .map(variable -> new SnmpRecord(
                        variable.getOid().toString(),
                        variable.toValueString()
                ))
                .collect(Collectors.toList());
    }

    private static void writeToAvro(List<SnmpRecord> records) throws IOException {
        DatumWriter<GenericRecord> datumWriter = new GenericDatumWriter<>(snmpSchema());
        DataFileWriter<GenericRecord> dataFileWriter = new DataFileWriter<>(datumWriter);
        dataFileWriter.create(snmpSchema(), new File("snmp.avro"));

        for (SnmpRecord record : records) {
            GenericRecord genericRecord = new GenericData.Record(snmpSchema());
            genericRecord.put("oid", record.getIod());
            genericRecord.put("value", record.getValue());

            dataFileWriter.append(genericRecord);
        }

        dataFileWriter.close();
    }

    private static Schema snmpSchema() {
        return SchemaBuilder
                .record("SnmpRecord")
                .namespace("com.netcracker.model")
                .fields()
                .name("oid").type().stringType().noDefault()
                .name("value").type().stringType().noDefault()
                .endRecord();
    }
}
