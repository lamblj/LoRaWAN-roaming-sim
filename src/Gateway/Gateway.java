package Gateway;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Gateway {

    private final int BUFSIZE = 1024;
    private String networkID;
    private InetAddress networkServer;
    private final int nsPort = 666;
    final int MYPORT = 4445;
    byte[] buf = new byte[BUFSIZE];

    public Gateway(String networkID) {
        this.networkID = networkID;
    }

    private void listenService(){


        try {
            /* Create Socket */
            DatagramSocket socket = new DatagramSocket(MYPORT);
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            /* Endless loop waiting for client connections */
            while (true) {
                /* Open new thread for each new client connection */
                socket.receive(packet);

                System.out.println("Packet data " + new String(packet.getData()));
                new Thread(new MessageHandler(packet, networkServer)).start();
                Arrays.fill(buf,(byte)0);
            }

        } catch (IOException e) {
        }
    }


    private void EstablishEndpoint(InetAddress networkServer) {
        this.networkServer = networkServer;
    }


    public void Initialize() throws UnknownHostException {
        EstablishEndpoint(InetAddress.getByName("127.0.0.1"));
        listenService();
    }


    class MessageHandler implements Runnable {
        private DatagramPacket packet;
        private InetAddress networkServer;

        public MessageHandler(DatagramPacket packet, InetAddress networkServer) {
            this.packet = packet;
            this.networkServer = networkServer;
        }

        public void sendPacket(DatagramPacket packet) {
            try {
                DatagramSocket nsConnection = new DatagramSocket(6664);
                packet.setAddress(networkServer);
                packet.setPort(6665);
                nsConnection.send(packet);
                nsConnection.close();
            } catch (SocketException e) {
               e.printStackTrace();

            } catch (IOException e) {
                System.out.println("Packet could not be sent");
            }

        }


        @Override
        public void run() {
          //  System.out.println(new String(packet.getData()));
            sendPacket(packet);
            Thread.currentThread().interrupt();
            //  DatagramPacket forwardPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), networkServer,nsPort);
            //  sendPacket(forwardPacket);

        }
    }
}




