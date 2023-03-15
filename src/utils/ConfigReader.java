package utils;

import java.io.FileInputStream;
import java.util.Properties;

public class ConfigReader {

    private String pathToConfig = "./";

    private Properties properties = new Properties();

    public ConfigReader(String pathToConfig) {
        this.pathToConfig = pathToConfig;
        try {
            properties.load(new FileInputStream(pathToConfig));
        } catch (Exception e) {
            System.out.println("Incorrect pathToConfig.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int getPort() {
        String portStr = properties.getProperty("port");

        int port = 0;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("Incorrect port value: " + portStr + ".");
            e.printStackTrace();
            System.exit(1);
        }

        return port;
    }

    public int getThreadLimit() {
        String threadLimitStr = properties.getProperty("thread_limit");

        int threadLimit = 0;
        try {
            threadLimit = Integer.parseInt(threadLimitStr);
        } catch (Exception e) {
            System.out.println("Incorrect thread_limit value: " + threadLimitStr + ". ");
            e.printStackTrace();
            System.exit(1);
        }

        return threadLimit;
    }

    public int getMaxRequestsInQueue() {
        String maxRequestsInQueueStr = properties.getProperty("max_requests_in_queue");

        int maxRequestsInQueue = 0;
        try {
            maxRequestsInQueue = Integer.parseInt(maxRequestsInQueueStr);
        } catch (Exception e) {
            System.out.println("Incorrect max_requests_in_queue value: " + maxRequestsInQueueStr + ". ");
            e.printStackTrace();
            System.exit(1);
        }

        return maxRequestsInQueue;
    }

    public String getPathToConfig() {
        return pathToConfig;
    }
}
