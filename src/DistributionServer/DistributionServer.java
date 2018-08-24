package DistributionServer;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;


public class DistributionServer {

    private static DatagramSocket receiveSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receiveBuffer;

    private static String collabDS;

    public static void main(String[] args) {

        // set up the receiving socket
        InetSocketAddress receiveLocalBindPoint = new InetSocketAddress(3001);
        try {
            receiveSocket = new DatagramSocket(receiveLocalBindPoint);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.out.println("Binding to port " + receiveLocalBindPoint.getPort());

        // set up DS-to-DS collaboration
        String ans = "";
        Scanner scanner = new Scanner(System.in);
        while (!ans.equals("y") || !ans.equals("n")) {
            System.out.println("Should the server initiate collaboration with another DS? (y/n)");
            ans = scanner.next();
        }
        if (ans.equals("y")) {
            // collect IP of collaborating DS from user
            System.out.println("Provide IP address of other DS: ");
            collabDS = scanner.next();

            try {
                // send collab. start request
                DatagramSocket sendSocket = new DatagramSocket();
                byte[] msg = "dctr start".getBytes();
                DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, Inet4Address.getByAddress(collabDS.getBytes()), 3001);
                sendSocket.send(sendPacket);
                System.out.println("Request sent to other DS");
                // receive response to start request
                receiveBuffer = new byte[65507];
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);
                System.out.println("Response received");
                String confirm = receivePacket.getData().toString().split(" ")[1].trim();
                // if other DS approves, we save it as collaborating DS in the database
                if (confirm.equals("allow")) {
                    System.out.println("Positive response, collaboration set up");
                    // save to db
                    DatabaseConnector dbc = new DatabaseConnector();
                    dbc.saveDSregistration(collabDS);
                }
                // if other DS says no, we continue without it
                else {
                    System.out.println("Not positive response, collaboration not set up");
                }

            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

