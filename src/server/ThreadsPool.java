package server;

import java.util.LinkedList;

class ThreadsPool {

    private final int maxRequestsInQueue;

    private final LinkedList<Runnable> requestsQueue;

    ThreadsPool(final int threadsPoolSize, final int maxRequestsInQueue) {
        this.maxRequestsInQueue = maxRequestsInQueue;
        this.requestsQueue = new LinkedList<>();

        for (int i = 0; i < threadsPoolSize; i++) {
            Thread requestsProcessThread = new Thread(new RequestProcessor());
            requestsProcessThread.start();
        }
    }

    public synchronized void addRequestToQueue(Runnable request) {
        while (requestsQueue.size() == maxRequestsInQueue) {
            try {
                wait();
            } catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
            }
        }
        requestsQueue.addLast(request);

        notifyAll();
    }

    private synchronized Runnable processRequest() {
        while (requestsQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                //Thread.currentThread().interrupt();
            }
        }
        Runnable request = requestsQueue.removeFirst();
        notifyAll();

        return request;
    }

    private final class RequestProcessor implements Runnable {

        @Override
        public void run() {
            while (true) {
                //System.out.println("requestsQueue size = " + requestsQueue.size());
                //System.out.println("Current Thread ID: " + Thread.currentThread().getId());

                processRequest().run();
            }
        }
    }
}