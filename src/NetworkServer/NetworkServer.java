package NetworkServer;

import java.io.IOException;
import java.net.*;

public class NetworkServer {

    private final int BUFSIZE = 1024;
    private int listenport = 6665;
    private int DSport = 3001;
    private String networkID;
    private byte[] buf;
    private String ipAddress;

    public NetworkServer(String ipAddress, String networkID) {
        this.networkID = networkID;
        this.ipAddress = ipAddress;

    }
    //NSCTR for command messages; Start netID or Stop netID commands
    //NSDAT for LoRaWAN messages


    private void listenService() throws IOException {

        /* Create Socket */
        // send roaming start request to DS
        DatagramSocket socket = new DatagramSocket(listenport);
        byte[] buf = ("nctr " + "start " + networkID).getBytes();
        DatagramPacket packet = new DatagramPacket(buf, buf.length, InetAddress.getByName(ipAddress), DSport);
        socket.send(packet);
        // try to get confirm from DS
        buf = new byte[1024];
        packet = new DatagramPacket(buf, buf.length);
        socket.receive(packet);
        String confirm = new String(packet.getData()).trim().split(" ")[1];
        System.out.println(confirm);
        if (confirm.equals("deny")) {
            return;
        }
        else if (confirm.equals("allow")) {
            System.out.println("DS allows roaming");
        }
        System.out.println("Listening for incoming connections");



        /* Endless loop waiting for client connections */
        while (true) {
            buf = new byte[BUFSIZE];
            packet = new DatagramPacket(buf,buf.length);
            /* Open new thread for each new client connection */
            socket.receive(packet);
            new Thread(new MessageHandler(packet, InetAddress.getByName(ipAddress), networkID,socket)).start();
        }


    }


    public void Initialize()  {
        try {
            listenService();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class MessageHandler implements Runnable {
        private DatagramPacket packet;
        private InetAddress distributionServer;
        private String networkID;
        private DatagramSocket socket;

        public MessageHandler(DatagramPacket packet, InetAddress distributionServer, String networkID, DatagramSocket socket) {
            this.packet = packet;
            this.distributionServer = distributionServer;
            this.networkID = networkID;
            this.socket = socket;
            System.out.println("Received packet data: " + new String(packet.getData()));
        }


        private boolean isLocal(String networkID) {

            if (this.networkID.equals(networkID) ) {
                return true;
            } else {
                return false;
            }
        }

        public void Handle(DatagramPacket packet) {
            //    try {
            if (isLocal(new String(packet.getData()).substring(0, 12)) ) {
                  System.out.println("Packet was not roaming, therefore it is not sent forwards");
            }
            else {
                try {

                    String dsFormat ="ndat " + (new String(packet.getData()));
                    System.out.println("Packet data : " +dsFormat);
                    packet.setData(dsFormat.getBytes());
                    packet.setPort(DSport);
                    packet.setAddress(distributionServer);
                    socket.send(packet);
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            //} //catch (SocketException e) {
            //  System.out.println("No connection could be established");

            //  }// catch (IOException e) {
            //  System.out.println("Packet could not be sent");
            //  }

        }

        @Override
        public void run() {
            Handle(packet);
            //  DatagramPacket forwardPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), networkServer,nsPort);
            //  sendPacket(forwardPacket);

        }
    }

}
