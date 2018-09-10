package Gateway;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class Gateway {

    private final int BUFSIZE = 1024;
    private String ipAddress;
    private int sendPort = 6665;
    private int MYPORT = 4445;
    byte[] buf = new byte[BUFSIZE];


    public static void main(String[] args)  {


        Gateway gw = new Gateway(); // Gateway IP
        gw.Initialize();


    }



    private void listenService() {
        try {
            /* Create Socket */
            DatagramSocket socket = new DatagramSocket(MYPORT);
            DatagramPacket packet = new DatagramPacket(buf, buf.length);
            System.out.println("Gateway is running and listening for connections");
            /* Endless loop waiting for client connections */
            while (true) {
                /* Open new thread for each new client connection */
                socket.receive(packet);
                System.out.println("Packet data " + new String(packet.getData()));
                new Thread(new MessageHandler(packet, InetAddress.getByName("127.0.0.1"),socket)).start();
                Arrays.fill(buf, (byte) 0);
            }

        } catch (IOException e) {
        }
    }

    public void Initialize() {
        listenService();
    }

    class MessageHandler implements Runnable {
        private DatagramPacket packet;
        private InetAddress networkServer;
        private DatagramSocket gateway;
        public MessageHandler(DatagramPacket packet, InetAddress networkServer, DatagramSocket gateway) {
            this.packet = packet;
            this.networkServer = networkServer;
            this.gateway = gateway;

        }

        public  void sendPacket(DatagramPacket packet) {
            try {
                DatagramPacket sendPacket = packet;
                sendPacket.setAddress(networkServer);
                sendPacket.setPort(sendPort);
                gateway.send(packet);
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




