package com.librato.watchconf.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.io.File;
import java.io.IOException;

public class WatchConf {

    public static void main(String[] args) throws Exception {
        if (args.length < 10) {
            printHelp();
            return;
        }

        pushConfig(args);
    }

    private static void pushConfig(String[] args) throws Exception {
        String zkConnectFlag = args[0];
        String formatFlag = args[2];
        String fileFlag = args[4];
        String outputFlag = args[6];
        String znodeFlag = args[8];

        if (!"-zkServer".equals(zkConnectFlag) || !"-format".equals(formatFlag) || !"-f".equals(fileFlag) || !"-o".equals(outputFlag) || !"-z".equals(znodeFlag)) {
            printHelp();
            return;
        }

        String zkServer = args[1];
        String format = args[3];
        String fileName = args[5];
        String output = args[7];
        String path = args[9];

        JsonNode jsonNode = getJsonNode(new File(fileName), format);

        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString(zkServer)
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        byte[] outputBytes = getOutputBytes(output, jsonNode);
        try {
            if(framework.checkExists().forPath(path) == null) {
                framework.create().creatingParentsIfNeeded().forPath(path, outputBytes);
            } else {
                framework.setData().forPath(path, outputBytes);
            }
            System.out.println("Successfully deployed configuration");
        } catch (Exception ex) {
            System.out.println("Error deploying config");
            ex.printStackTrace(System.err);
        }
    }

    private static byte[] getOutputBytes(String output, JsonNode jsonNode) throws JsonProcessingException {
        if ("yaml".equals(output)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.writeValueAsBytes(jsonNode);
        } else if ("json".equals(output)) {
            return jsonNode.toString().getBytes();
        } else {
            printHelp();
            System.exit(1);
            return null;
        }
    }

    private static JsonNode getJsonNode(File file, String format) throws IOException {
        if ("yaml".equals(format)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            return mapper.readTree(file);
        } else if ("json".equals(format)) {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readTree(file);
        } else {
            printHelp();
            System.exit(1);
            return null;
        }
    }

    private static void printHelp() {
        System.out.println("watchconf: Must specify -zkServer <host:port> and additional required flags");
        System.out.println("-format [yaml|json]: input file format")
        System.out.println("-f <file>: input file to read from");
        System.out.println("-o [yaml|json]: format of data to output to znode");
        System.out.println("-z: full path to znode to update, will create parents and node doesn't exist");
    }
}
