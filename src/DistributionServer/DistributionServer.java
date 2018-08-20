package DistributionServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class DistributionServer {

    private static DatagramSocket receiveSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receiveBuffer;

    public static void main (String[] args) {
        try {
            initialize();
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);
                DatagramPacket processCopy = new DatagramPacket(receivePacket.getData(), receivePacket.getLength());
                Thread processThread = new Thread(new MessageHandler(processCopy));
                processThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void initialize() throws SocketException {

        // set up the receiving socket
        InetSocketAddress receiveLocalBindPoint = new InetSocketAddress(3001);
        receiveSocket = new DatagramSocket(receiveLocalBindPoint);
        System.out.println("Binding to port " + receiveLocalBindPoint.getPort());

        receiveBuffer = new byte[65507];

        // check if DB exists
        //      if not
        //          create it
        //          connect to it
        //      if yes
        //          connect to it


        System.out.println("Distribution Server good to go");
    }
}

class MessageHandler implements Runnable {

    private DatagramPacket receivePacket;

    public MessageHandler(DatagramPacket receivePacket) {
        this.receivePacket = receivePacket;
    }

    @Override
    public void run() {
        String message = new String(receivePacket.getData());
        String[] messageParts = message.split(" ");
        switch (messageParts[0]) {
            case "ndat": handleNDAT(messageParts);
                break;
            case "nctr": handleNCTR(messageParts);
                break;
            case "ddat": handleDDAT(messageParts);
                break;
            case "dctr": handleDCTR(messageParts);
                break;
            case "dinf": handleDINF(messageParts);
                break;
            default: System.out.println("Default");
                break;
        }
    }

    private void handleNDAT(String[] messageParts) {
        System.out.println("Data from NS: " + messageParts[1]);
        // extract NetID
        // lookup NetID in database
        // forward message to matched IP
    }

    private void handleNCTR(String[] messageParts) {
        System.out.println("Control from NS: " + messageParts[1] + " " + messageParts[2]);
        // check if start or stop
        // if start
        //      evaluate request
        //          if approved
        //              save data
        //              send confirm
        //          if denied
        //              send deny
        // if stop
        //      delete data
        //      send confirm
    }

    private void handleDDAT(String[] messageParts) {
        System.out.println("Data from DS");
        // extract NetID
        // lookup NetID in database
        // forward message to matched IP
    }

    private void handleDCTR(String[] messageParts) {
        System.out.println("Control from DS");
        // check if start or stop
        // if start
        //      evaluate request
        //          if approved
        //              save data
        //              send confirm
        //          if denied
        //              send deny
        // if stop
        //      delete data
        //      send confirm
    }

    private void handleDINF(String[] messageParts) {
        System.out.println("Info from DS");
        // get IP of DS that is sending the update
        // get list of IPs from update
        // compare to currently stored IPs from that DS
        // make necessary changes

    }

}