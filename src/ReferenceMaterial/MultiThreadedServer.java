package ReferenceMaterial;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class MultiThreadedServer {

    public static final int MYPORT = 4950;

    public static void main(String[] args) throws IOException {
        MultiThreadedServer server = new MultiThreadedServer();
        server.run(args);
    }

    public void run(String[] args) {
        try {
            /* Create Socket */
            ServerSocket socket = new ServerSocket(MYPORT);
			
			/* Endless loop waiting for client connections */
            while (true) {
				/* Open new thread for each new client connection */
                new Thread(new ConnectionHandler(socket.accept())).start();
				
				/* Print out to investigate open threads */
                printThreadsInfo();
            }
        } catch (IOException e) {
        }
    }

    /* Prints the status of the currently active threads in the console */
    private void printThreadsInfo() {
        System.out.println("******************************************");
        System.out.println("Active Threads:" + Thread.activeCount());
        Thread[] threads = new Thread[Thread.activeCount()];
        Thread.enumerate(threads);
        for (Thread t : threads)
            System.out.println(t.toString());
        System.out.println("******************************************");
    }

    /* Handles client connection */
    class ConnectionHandler implements Runnable {
        private Socket connection;

        public ConnectionHandler(Socket connection) {
            this.connection = connection;
        }

        @Override
        public void run() {

            
        }

    }


}
