package DistributionServer;

import java.io.IOException;
import java.net.*;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class DistributionServer {

    private static DatagramSocket receiveSocket;
    private static DatagramSocket sendSocket;
    private static DatagramPacket receivePacket;
    private static byte[] receiveBuffer;

    private static String collabDS;

    public static void main(String[] args) {

        if(args.length > 1) {
            System.out.println("Provide zero arguments to set up DS without collaboration, or one argument, the IP of the other DS to set up DS with collaboration");
        }

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
        String ans;
        if(args.length == 0) {
            ans = "n";
        }
        else {
            ans = "y";
        }
        if (ans.equals("y")) {
            collabDS = args[0];

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

        // loop for 70 minutes listening for and processing incoming messages
        int processedMessages = 0;
        long endTime = System.currentTimeMillis() + 4200000 ;
        while (System.currentTimeMillis() < endTime) {
            try {
                // set up for receiving UDP message
                receiveBuffer = new byte[65507];
                receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                receiveSocket.receive(receivePacket);

                System.out.println("\n*************************");
                System.out.println("* MESSAGE RECEIVED");
                System.out.println("* CONTENT: " + new String(receivePacket.getData()).trim());
                System.out.println("* AT: " + LocalDateTime.now().toString());
                processedMessages++;
                System.out.println("* MESSAGES PROCESSED: " + processedMessages);
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
        System.out.println("Total number of roaming messages processed: " + processedMessages);
    }

    private static void handleNCTR(DatagramPacket receivePacket) {

        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String sourceNetID = messageParts[2].trim();
        System.out.println("* FROM: NS " + sourceNetID);
        // if it is a start request
        if (cmd.equals("start")){
            System.out.println("* TYPE: NCTR START");
            String NSIPaddr = receivePacket.getAddress().getHostAddress();
            // save data
            dbc.saveNSregistration(sourceNetID, NSIPaddr);
            System.out.println("* RESULT: NS registered for roaming");

            // send confirm
            sendConfirm(receivePacket, "nctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            System.out.println("* TYPE: NCTR STOP");
            // delete data
            dbc.deleteNSregistration(sourceNetID);
            System.out.println("* RESULT: NS stopped roaming");

            // send confirm
            sendConfirm(receivePacket, "nctr allow");
        }
        else {
            System.out.println("* RESULT: error");
            // neither start nor stop, error
        }
        // send update to collaborating DSs in DINF format
        sendDINF();
        dbc.close();
    }

    private static void handleNDAT(DatagramPacket receivePacket) {
        System.out.println("* TYPE: NDAT");
        DatabaseConnector dbc = new DatabaseConnector();
        String[] messageParts = new String(receivePacket.getData()).split(" ");

        // extract NetID
        String targetNetID = messageParts[1];
        System.out.println("* TARGET: NS " + targetNetID);
        // check if the target NS is served here
        String IP = dbc.lookupNSIPaddr(targetNetID);
        if (IP.equals("error")) {
            System.out.println("* INTERMEDIATE: target NS not served by this DS");
            // NS not served by this DS, check if a collaborating DS is serving it.
            String servingDSIP = dbc.lookupDSservingNetID(targetNetID);
            if (servingDSIP.equals("error")) {
                System.out.println("* INTERMEDIATE: a DS serving this NS could not be found");
                System.out.println("* RESULT: dropping message");
                // a DS that serves the specified NetID was not found, drop packet
                return;
            } else {
                // a DS was found, forward the message to that in DDAT format
                System.out.println("* INTERMEDIATE: a DS serving this NS was found");
                String message = "ddat " + targetNetID + " " + messageParts[2].trim();
                try {
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(servingDSIP), 3001);
                    sendSocket.send(sendPacket);
                    System.out.println("* RESULT: forwarded message to DS at " + servingDSIP);
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("* INTERMEDIATE: target NS is served by this DS");
            // forward to matched NS served by this DS
            try {
                String message = messageParts[1] + " " + messageParts[2];
                DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(IP), 6665);
                sendSocket.send(sendPacket);
                System.out.println("* RESULT: forwarded message to NS " + targetNetID);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dbc.close();
    }

    private static void handleDCTR(DatagramPacket receivePacket) {

        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String DSIPaddr = receivePacket.getAddress().getHostAddress();
        // if it is a start request
        if (cmd.equals("start")){
            System.out.println("* TYPE: DCTR START");
            // save data
            dbc.saveDSregistration(DSIPaddr);
            System.out.println("* RESULT: DS registered for collaboration.");

            // send confirm
            sendConfirm(receivePacket, "dctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            System.out.println("* TYPE: DCTR STOP");
            // delete data
            dbc.deleteDSregistration(DSIPaddr);
            System.out.println("* RESULT: DS stopped collaborating.");

            // send confirm
            sendConfirm(receivePacket, "dctr allow");
        }
        else {
            // neither start nor stop, error
            System.out.println("* INTERMEDIATE: invalid command");
            System.out.println("* RESULT: dropping message");
        }
        sendDINF();
        dbc.close();
    }

    private static void handleDINF(DatagramPacket receivePacket) {
        System.out.println("* TYPE: DINF");

        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();
        LinkedList<String> updatedNetIDs = new LinkedList<>();

        // get IP of DS that is sending the update
        String DSIP = receivePacket.getAddress().getHostAddress();

        System.out.println("* FROM: DS at " + DSIP);
        // get list of NetIDs from packet
        if (messageParts.length > 1) {
            for (int i = 1; i < messageParts.length; i++) {
                updatedNetIDs.add(messageParts[i].trim());
            }
        }
        // apply DB updates
        dbc.updateRegisteredNSbyDS(DSIP, updatedNetIDs);
        System.out.println("* RESULT: updates saved to DB");

        dbc.close();
    }

    private static void handleDDAT(DatagramPacket receivePacket) {
        System.out.println("* TYPE: DDAT");
        DatabaseConnector dbc = new DatabaseConnector();
        System.out.println("* FROM: DS at " + receivePacket.getAddress());

        // extract NetID
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        String NetID = messageParts[1];
        System.out.println("* TARGET: NS " + NetID);

        // lookup NS IP in database
        String IP = dbc.lookupNSIPaddr(NetID);

        // forward message to matched IP
        // should only forward target NetID space LoRaWAN data
        String message = messageParts[1] + " " + messageParts[2];
        try {
            DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(IP), 6665);
            sendSocket.send(sendPacket);
            System.out.println("* RESULT: message forwarded to " + NetID);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        dbc.close();
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

            System.out.println("* CONFIRM: sent to " + receivePacket.getAddress() + port);
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

        System.out.println("\n*************************");
        System.out.println("* SENDING DINF");
        System.out.println("* #NSs: " + NetIDs.size());

        // send NetID list to every DS in DINF format
        String message = "dinf ";
        for (String s : NetIDs) {
            message += s + " ";
        }
        message.trim();
        byte[] buf = message.getBytes();
        for (String s : DSIPs) {
            try {
                DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, InetAddress.getByName(s), 3001);
                sendSocket.send(sendPacket);
                System.out.println("* TO: DS at " + s);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        dbc.close();
    }
}
