package com.librato.watchconf.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.ImmutableMap;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class WatchConf {

    public static void main(String[] args) throws Exception {
        if (args.length < 10) {
            printHelp();
            return;
        }

        pushConfig(args);
    }

    private static void pushConfig(String[] args) throws Exception {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (int i = 0; i < args.length; i++) {
            if (i % 2 == 0 && i + 1 < args.length) {
                builder.put(args[i], args[i + 1]);
            }
        }

        Map<String, String> argMap = builder.build();

        String zkServer = argMap.get("-zkServer");
        String format = argMap.get("-format");
        String fileName = argMap.get("-f");
        String output = argMap.get("-o");
        String path = argMap.get("-z");
        if (zkServer == null || format == null || fileName == null || output == null || path == null) {
            printHelp();
            return;
        }


        JsonNode jsonNode = getJsonNode(new File(fileName), format);

        CuratorFramework framework = CuratorFrameworkFactory.builder()
                .connectionTimeoutMs(1000)
                .connectString(zkServer)
                .retryPolicy(new ExponentialBackoffRetry(1000, 5))
                .build();
        framework.start();

        byte[] outputBytes = getOutputBytes(output, jsonNode);

        String className = argMap.get("-c");
        if (className != null) {
            Class clazz = Class.forName(className);
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                objectMapper.readValue(outputBytes, clazz);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                System.out.println("error converting data to class: " + className + " please fix input error before continuing");
                return;
            }
        }

        try {
            if (framework.checkExists().forPath(path) == null) {
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
        System.out.println("-format [yaml|json]: input file format");
        System.out.println("-f <file>: input file to read from");
        System.out.println("-o [yaml|json]: format of data to output to znode");
        System.out.println("-z full path to znode to update, will create parents and node doesn't exist");
        System.out.println("-c (Optional) name of class to validate JSON against before pushing");
    }
}
