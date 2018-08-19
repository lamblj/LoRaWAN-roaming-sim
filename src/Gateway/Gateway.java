package Gateway;

import java.io.IOException;
import java.net.*;

public class Gateway {

    private final int BUFSIZE = 1024;
    private String networkID;
    private InetAddress networkServer;
    private final int nsPort = 666;
    final int MYPORT = 4445;


    public Gateway(String networkID){
        this.networkID = networkID;
    }

    private void listenService() throws SocketException {


             try {
                 /* Create Socket */
                 DatagramSocket socket = new DatagramSocket(MYPORT);

                 /* Endless loop waiting for client connections */
                 while (true) {
                     /* Open new thread for each new client connection */
                     new Thread(new MessageHandler(socket, networkServer)).start();

                 }

             } catch (IOException e) {
             }
         }









    private void EstablishEndpoint(InetAddress networkServer) {
        this.networkServer = networkServer;
    }


    public void Initialize() throws SocketException, UnknownHostException {
        EstablishEndpoint(InetAddress.getByName("127.0.0.1"));
        listenService();
    }


    class MessageHandler implements Runnable {
        private DatagramSocket connection;
        private InetAddress networkServer;

        public MessageHandler(DatagramSocket connection, InetAddress networkServer) {
            this.connection = connection;
            this.networkServer = networkServer;
        }

        public void sendPacket(DatagramPacket packet){
            try {
                DatagramSocket nsConnection = new DatagramSocket(nsPort);
                nsConnection.send(packet);
                nsConnection.close();
            }
            catch (SocketException e){
                System.out.println("No connection could be established");

            }
            catch (IOException e){
             System.out.println("Packet could not be sent");
            }

        }



        @Override public void run() {
            byte[] buf = new byte[BUFSIZE];
            DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
            try {


                connection.receive(receivePacket);
                System.out.println(new String(receivePacket.getData()));
              //  DatagramPacket forwardPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), networkServer,nsPort);
              //  sendPacket(forwardPacket);


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}




