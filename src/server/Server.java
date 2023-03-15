package server;

import java.net.ServerSocket;
import java.net.Socket;

public class Server {

    private final int port;

    private final ThreadsPool threadsPool;

    public Server(int port, int threadsPoolSize, int maxRequestsInQueue) {
        this.port = port;
        this.threadsPool = new ThreadsPool(threadsPoolSize, maxRequestsInQueue);
    }

    public void run() {
        runSocketRequestsLooper();
    }

    private void runSocketRequestsLooper() {
        try (ServerSocket serverSocket = new ServerSocket(this.port)) {
            while (true) {
                try {
                    Socket socketRequest = serverSocket.accept();
                    SocketRequestHandler socketRequestHandler = new SocketRequestHandler(socketRequest);
                    threadsPool.addRequestToQueue(socketRequestHandler);
                } catch (Exception e) {
                    System.out.println("Cant accept process socket request on port: " + this.port + ".");
                    e.printStackTrace();
                    //System.exit(1);
                }
            }
        } catch (Exception e) {
            System.out.println("Cant create socket on port: " + this.port + ".");
            e.printStackTrace();
            System.exit(1);
        }
    }
}