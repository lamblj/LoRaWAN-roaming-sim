package DistributionServer;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class DistributionServer2 {

    private static DatagramSocket receiveSocket;
    private static DatagramSocket sendSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receiveBuffer;

    private static String collabDS;

    public static void main(String[] args) {

        // set up sockets
        try {
            receiveSocket = new DatagramSocket(3001);
            sendSocket = new DatagramSocket(3002);
            System.out.println("Sockets successfully set up.");
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        // set up DS-to-DS collaboration
        String ans = "";
        Scanner scanner = new Scanner(System.in);
        System.out.println("Should the server initiate collaboration with another DS? (y/n)");
        ans = scanner.next();
        if (ans.equals("y")) {
            // collect IP of collaborating DS from user
            System.out.println("Provide IP address of other DS: ");
            collabDS = scanner.next();

            try {
                // send collab. start request
                byte[] msg = "dctr start".getBytes();
                DatagramPacket sendPacket = new DatagramPacket(msg, msg.length, Inet4Address.getByName(collabDS), 3001);
                sendSocket.send(sendPacket);
                System.out.println("Request sent to other DS");
                // receive response to start request
                receiveBuffer = new byte[65507];
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);
                System.out.println("Response received");
                String confirm = new String(receivePacket.getData()).split(" ")[1].trim();
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
        else if (ans.equals("n")) {
            System.out.println("Continuing without setting up collaboration");
        }
        else {
            System.out.println("Invalid response, continuing without setting up collaboration");
        }
        System.out.println("Distribution Server good to go");

        // endless loop listening for and processing incoming messages
        while (true) {
            try {
                // set up for receiving UDP message
                receiveBuffer = new byte[65507];
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);

                // check message type
                String messagetype = new String(receivePacket.getData()).split(" ")[0].trim();

                // handle message
                switch (messagetype) {
                    case "ndat": handleNDAT(receivePacket);
                        break;
                    case "nctr": handleNCTR(receivePacket);
                        break;
                    case "ddat": handleDDAT(receivePacket);
                        break;
                    case "dctr": handleDCTR(receivePacket);
                        break;
                    case "dinf": handleDINF(receivePacket);
                        break;
                    default: System.out.println("Default");
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleNCTR(DatagramPacket receivePacket) {
        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String NetID = messageParts[2].trim();
        // if it is a start request
        if (cmd.equals("start")){
            String NSIPaddr = receivePacket.getAddress().getHostAddress();
            // save data
            dbc.saveNSregistration(NetID, NSIPaddr);
            System.out.println("NS with ID " + NetID + ", and IP address " + NSIPaddr + " registered for roaming.");

            // send confirm
            sendConfirm(receivePacket, "nctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            // delete data
            dbc.deleteNSregistration(NetID);
            System.out.println("NS with ID " + NetID + " stopped roaming.");

            // send confirm
            sendConfirm(receivePacket, "nctr allow");
        }
        else {
            // neither start nor stop, error
        }
        // send update to collaborating DSs in DINF format
        sendDINF();

    }

    private static void handleNDAT(DatagramPacket receivePacket) {
        DatabaseConnector dbc = new DatabaseConnector();
        String[] messageParts = new String(receivePacket.getData()).split(" ");

        // extract NetID
        String NetID = messageParts[1];
        // check if the target NS is served here
        String IP = dbc.lookupNSIPaddr(NetID);
        if (IP.equals("error")) {
            // NS not served by this DS, check if a collaborating DS is serving it.
            String servingDSIP = dbc.lookupDSservingNetID(NetID);
            if (servingDSIP.equals("error")) {
                System.out.println("Can't forward packet, dropping it.");
                // a DS that serves the specified NetID was not found, drop packet
                return;
            } else {
                // a DS was found, forward the message to that in DDAT format
                String message = "ddat " + NetID + " " + messageParts[2].trim();
                try {
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(servingDSIP), 3001);
                    sendSocket.send(sendPacket);
                    System.out.println("Forwarded message to DS at " + servingDSIP);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // forward to matched NS served by this DS
            try {
                DatagramPacket sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getByName(IP), 6665);
                sendSocket.send(sendPacket);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void handleDCTR(DatagramPacket receivePacket) {

        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String DSIPaddr = receivePacket.getAddress().getHostAddress();
        // if it is a start request
        if (cmd.equals("start")){

            // save data
            dbc.saveDSregistration(DSIPaddr);
            System.out.println("DS with IP " + DSIPaddr + " registered for collaboration.");

            // send confirm
            sendConfirm(receivePacket, "dctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            // delete data
            dbc.deleteDSregistration(DSIPaddr);
            System.out.println("DS with IP " + DSIPaddr + " stopped collaborating.");

            // send confirm
            sendConfirm(receivePacket, "dctr allow");
        }
        else {
            // neither start nor stop, error
        }
        sendDINF();
    }

    private static void handleDINF(DatagramPacket receivePacket) {

        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();
        LinkedList<String> updatedNetIDs = new LinkedList<>();

        // get IP of DS that is sending the update
        String DSIP = receivePacket.getAddress().getHostAddress();

        System.out.println("DINF from " + DSIP);
        // get list of NetIDs from packet
        if (messageParts.length > 1) {
            for (int i = 1; i < messageParts.length; i++) {
                updatedNetIDs.add(messageParts[i].trim());
            }
        }
        // apply DB updates
        dbc.updateRegisteredNSbyDS(DSIP, updatedNetIDs);
    }

    private static void handleDDAT(DatagramPacket receivePacket) {
        DatabaseConnector dbc = new DatabaseConnector();

        // extract NetID
        String NetID = new String(receivePacket.getData()).split(" ")[1];
        // lookup NS IP in database
        String IP = dbc.lookupNSIPaddr(NetID);
        // forward message to matched IP
        try {
            DatagramPacket sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getByName(IP), 6665);
            sendSocket.send(sendPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendConfirm(DatagramPacket receivePacket, String message) {
        try {
            byte[] buf = message.getBytes();
            String[] messageparts = new String(receivePacket.getData()).split(" ");
            int port = 0;
            if (messageparts[0].equals("dctr")) {
                port = 3001;
            }
            else {
                port = receivePacket.getPort();
            }
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, receivePacket.getAddress(), port);
            sendSocket.send(sendPacket);

            System.out.println("confirm message sent to " + receivePacket.getAddress() + port);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendDINF() {
        DatabaseConnector dbc = new DatabaseConnector();

        // get list of NetIDs served by this DS
        List<String> NetIDs = dbc.getNSregistrations();
        // if no NSs are registered, work with an empty list
        if (NetIDs.size() == 0) {
            NetIDs = new LinkedList<String>();
        }

        // get list of collaborating DSs
        List<String> DSIPs = dbc.getDSregistrations();
        // if no DSs are collaborating, there is no server to send the update to
        if (DSIPs.size() == 0) {
            return;
        }

        // send NetID list to every DS in DINF format
        String message = "dinf ";
        for (String s : NetIDs) {
            message += " " + s;
        }
        byte[] buf = message.getBytes();
        for (String s : DSIPs) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(s), 3001);
                sendSocket.send(sendPacket);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
