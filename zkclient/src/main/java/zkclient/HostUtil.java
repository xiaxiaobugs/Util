package zkclient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class HostUtil {
    private String path = "/etc/hosts"; //write host-address entry into /etc/hosts file

    private final String sep = "\t";
    private String originPart="";

    private final static Logger LOG = LoggerFactory.getLogger(HostUtil.class);

    HostUtil(String path) {
        this.path = path;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            char[] buf = new char[1024];
            while (reader.read(buf) >= 0) {
                String bufStr = String.valueOf(buf);
                bufStr =  bufStr.replaceAll("\\\0", "");
                if (!bufStr.startsWith("#")) {
                    originPart += bufStr;
                }
            }
            LOG.info(originPart);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param hosts Hosts-Address mapping map
     *
     */
    void updateHosts(HashMap<String, String> hosts) {
        try {
            FileWriter writer = new FileWriter(path, false);
            writer.write(originPart);
            for (Map.Entry<String, String> entry : hosts.entrySet()) {
                String line = entry.getValue() + "\t" + entry.getKey() + "\n";
                LOG.info("write >>>>   " + entry.getKey() + "," + entry.getValue() + ">");
                writer.write(line);
            }
            writer.close();
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

}
