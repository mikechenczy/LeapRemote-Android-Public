package org.mj.leapremote.util;

import com.alibaba.fastjson.JSON;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.util.List;

public class GetTextRecord {
    public static void main(String[] args) {
    }

    public static String getATxt(String domain) {
        try {
            Lookup lookup = new Lookup(domain, Type.TXT);
            Record[] records = lookup.run();
            if (records==null||records.length==0)
                return "";
            TXTRecord txtRecord = (TXTRecord) records[0];
            StringBuilder result = new StringBuilder();
            for (String str : (List<String>)txtRecord.getStrings()) {
                result.append(str);
            }
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}