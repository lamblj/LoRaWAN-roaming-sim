package Lab1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Instant;

/**
 * TCP echo server, handles multi client
 * Created by Michael on 01/02/2017.
 */
public class TCPEchoServer {

    //how long the client thread should wait for something to read before closing
    private static final int SERVER_TIME_OUT = 1500;
    private static final int BUFSIZE = 1024;
    private static final int MYPORT = 1337;

    public static void main(String[] args) throws IOException {

        System.out.println("Running TCP Server...");

        /* Creating connection socket  */
        ServerSocket socket = new ServerSocket(MYPORT);

        /* Creating multi connections threads */
        while (true) {
            Socket client = socket.accept();
            ClientThread thread = new ClientThread(client);
            thread.start();
        }

    }

    /**
     * HandleControl class that handles a client connection.
     * Thread has a timeout it waits for input in stream, if no input until timeout
     * we close the socket.
     */
    private static class ClientThread extends Thread {

        private static int clientNextID = 1;
        private int ID;
        private Socket clientSocket;
        private InputStream fromClient;
        private OutputStream backToClient;

        public ClientThread(Socket clientSocket) {

            ID = clientNextID++;
            this.clientSocket = clientSocket;

        }

        public void run() {

            try {

                fromClient = clientSocket.getInputStream();
                backToClient = clientSocket.getOutputStream();

                while (true) {
                    Instant serverTimeout = Instant.now().plusMillis(SERVER_TIME_OUT);

                    /* Wait in while loop for something in stream or until we hit server timeout */
                    while (fromClient.available() == 0 && !Instant.now().isAfter(serverTimeout));
                    if (Instant.now().isAfter(serverTimeout)) {
                        break;
                    }

                    byte[] buffer = new byte[BUFSIZE];

                    StringBuilder echo = new StringBuilder();

                    while (fromClient.available() != 0) {

                        fromClient.read(buffer);
                        echo.append(new String(buffer));

                        /* If stream is empty after read, timeout wait for another packet */

                    }
                    System.out.printf("TCP echo request from client %s @ %s", ID, clientSocket.getInetAddress());
                    System.out.printf(" using port %d\n", clientSocket.getPort());

                    /* Trim whitespaces at the end before we send the message */
                    backToClient.write(echo.toString().trim().getBytes());

                }
                System.out.println("Client "+ ID + " from port " + clientSocket.getPort() + " is closing");
                clientNextID--;

                fromClient.close();
                backToClient.close();
                clientSocket.close();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
