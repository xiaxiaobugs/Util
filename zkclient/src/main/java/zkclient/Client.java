package zkclient;

import org.I0Itec.zkclient.IZkDataListener;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Pattern;


public class Client {

    private static final Logger LOG = LoggerFactory.getLogger(Client.class);

    public static void main(String[] args) throws InterruptedException, UnknownHostException {
        System.out.println("Env:");
        System.out.println("   " +System.getenv());
        String pattern = "([0-9]{1,3}\\.){3}[0-9]{1,3}:[0-9]{1,5}";
        pattern = pattern + "(," + pattern +")*";

        String server = System.getenv("ZOOKEEPER_SERVER");

        final HostUtil myHost = new HostUtil("/etc/hosts");
        if(server == null) {
            LOG.error("need to set ZOOKEEPER_SERVER env!");
            server = "172.17.0.1:2181,172.17.0.1:2182,172.17.0.1:2183";
        }
        if (!Pattern.matches(pattern, server)) {
            LOG.warn("ZOOKEEPER_SERVER cannot found! Need to set env: ZOOKEEPER_SERVER, <ip>:port[,<ip>:port]");
            System.exit(1);
        }
        String setKeysString = System.getenv("HOSTNAME");
        String[] setKeys = setKeysString.split(",");
        String ip = InetAddress.getLocalHost().getHostAddress();

        final ZkClient zkClient = new ZkClient(server,30000);
        LOG.info("Successfully connect to " + server);

        final String path = "/service";
        if (!zkClient.exists(path)) {
            zkClient.createPersistent(path);
            LOG.info(path + " not exist, create one");
        }

        // watching
        zkClient.subscribeDataChanges(path, new IZkDataListener() {
            private final Logger LOG = LoggerFactory.getLogger(IZkDataListener.class);
            public void handleDataChange(String dataPath, Object data) {
                HashMap<String, String> hosts = new HashMap<>();
                ArrayList<String> children = (ArrayList<String>) zkClient.getChildren(path);
                for (String child : children) {
                    hosts.put(child, zkClient.readData(path + "/" + child));
                }
                LOG.info("Cluster data is modified! Update local configuration");
                myHost.updateHosts(hosts);
            }
            public void handleDataDeleted(String dataPath) {
                LOG.warn("Node Deleted");
            }
        });

        // Add to Cluster
        for (String host : setKeys) {
            if (!zkClient.exists(path + "/" + host)) {
                zkClient.createPersistent(path + "/" + host, ip);
            } else {
                zkClient.writeData(path + "/" + host, ip);
            }
            LOG.info("Update Node " + host + " to " + ip);
        }
        zkClient.writeData(path, "Updated by " + ip + "At " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date()));
        //long running
        while (true) {
            Thread.sleep(60000);
        }
    }
}
