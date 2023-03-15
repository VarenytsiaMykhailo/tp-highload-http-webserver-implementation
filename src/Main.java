import server.Server;
import utils.ConfigReader;

public class Main {
    private static final String pathToConfig = "etc/httpd.conf";

    public static void main(String[] args) {
        ConfigReader configReader = new ConfigReader(pathToConfig);
        Server server = new Server(
                configReader.getPort(),
                configReader.getThreadLimit(),
                configReader.getMaxRequestsInQueue()
        );

        server.run();
    }
}