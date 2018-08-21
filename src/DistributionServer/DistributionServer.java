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

    public static void main(String[] args) {

        // set up the receiving socket
        InetSocketAddress receiveLocalBindPoint = new InetSocketAddress(3001);
        try {
            receiveSocket = new DatagramSocket(receiveLocalBindPoint);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("Binding to port " + receiveLocalBindPoint.getPort());
        System.out.println("Distribution Server good to go");

        while (true) {
            try {
                receiveBuffer = new byte[65507];
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);
                DatagramPacket processCopy = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), receivePacket.getAddress(), receivePacket.getPort());
                Thread processThread = new Thread(new MessageHandler(processCopy));
                processThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

