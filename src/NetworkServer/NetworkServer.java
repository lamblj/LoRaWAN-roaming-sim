package NetworkServer;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class NetworkServer {

    private final int BUFSIZE = 1024;
    private int listenport = 6665;
    private int sendPort = 9999;
    private String networkID = "bbc20180820K";
    private byte[] buf = new byte[BUFSIZE];
    private InetAddress DS = InetAddress.getByName("127.0.0.1");
    public NetworkServer() throws UnknownHostException {


    }
    //NSCTR for command messages; Start netID or Stop netID commands
    //NSDAT for LoRaWAN messages

    private void EstablishEndpoint(InetAddress distributionServer, String networkID){
        this.DS = distributionServer;

        }


    private void listenService() throws IOException {

            InetAddress distributionServer = InetAddress.getByName("127.0.0.1");

            /* Create Socket */
        DatagramSocket socket = new DatagramSocket(listenport);
        byte[] tempBuf = ("NSCTR " + "Start " + networkID).getBytes();
        DatagramPacket packet = new  DatagramPacket(tempBuf, tempBuf.length, distributionServer, listenport);
        System.out.println("Listening for incoming connections");
        System.out.println(new String(packet.getData()));
        socket.send(packet);
        packet.setData(buf);
        EstablishEndpoint(distributionServer, networkID);
        Arrays.fill(buf,(byte)0);
            /* Endless loop waiting for client connections */
            while (true) {
                /* Open new thread for each new client connection */
                socket.receive(packet);

                new Thread(new MessageHandler(packet, distributionServer, networkID)).start();
                Arrays.fill(buf,(byte)0);
            }



    }


    public void Initialize() throws IOException {
        listenService();
    }


    class MessageHandler implements Runnable {
        private DatagramPacket packet;
        private InetAddress distributionServer;
        private String networkID;

        public MessageHandler(DatagramPacket packet, InetAddress distributionServer, String networkID) {
            this.packet = packet;
            this.distributionServer = distributionServer;
            this.networkID = networkID;
        }


        private boolean isLocal(String networkID) {

            if (this.networkID == networkID) {
                return true;
            } else {
                return false;
            }
        }

        public void Handle(DatagramPacket packet) {
        //    try {
               // DatagramSocket DSConnection = new DatagramSocket(666);
                if (isLocal(packet.toString().substring(0, 12))) {
                  //  System.out.println("Packet was not roaming, therefore it is not sent forwards");
                }

                else {
                    System.out.println("Handling packet  " + new String(packet.getData()));
                    packet.setPort(listenport);
                    packet.setAddress(distributionServer);
                   // System.out.println("Packet sent,but no DS");
                   // DSConnection.send(packet);
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
            Thread.currentThread().interrupt();
            //  DatagramPacket forwardPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), networkServer,nsPort);
            //  sendPacket(forwardPacket);

        }
    }

}