package DistributionServer;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;

public class MessageHandler implements Runnable {

    private DatagramPacket receivePacket;
    private final int SENDPORT = 3002;

    public MessageHandler(DatagramPacket receivePacket) {
        this.receivePacket = receivePacket;
    }

    @Override
    public void run() {
        String messagetype = new String(receivePacket.getData()).split(" ")[0];
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
    }

    private void handleNCTR(DatagramPacket receivePacket) {
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
            sendConfirm("nctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            // delete data
            dbc.deleteNSregistration(NetID);
            System.out.println("NS with ID " + NetID + " stopped roaming.");

            // send confirm
            sendConfirm("nctr allow");
        }
        else {
            // neither start nor stop, error
        }
        // send update to collaborating DSs in DINF format
        sendDINF();

    }

    private void handleNDAT(DatagramPacket receivePacket) {
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
                // a DS that serves the specified NetID was not found, drop packet
                return;
            } else {
                // a DS was found, forward the message to that in DDAT format
                String message = "ddat " + NetID + " " + messageParts[2].trim();
                try {
                    DatagramSocket sendSocket = new DatagramSocket(SENDPORT);
                    DatagramPacket sendPacket = new DatagramPacket(message.getBytes(), message.length(), InetAddress.getByName(servingDSIP), 3001);
                    sendSocket.send(sendPacket);
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
                DatagramSocket sendSocket = new DatagramSocket(SENDPORT);
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

    private void handleDCTR(DatagramPacket receivePacket) {

        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String DSIPaddr = receivePacket.getAddress().getHostAddress();
        // if it is a start request
        if (cmd.equals("start")){

            // save data
            dbc.saveDSregistration(DSIPaddr);


            // send confirm
            sendConfirm("dctr allow");

            System.out.println("DS with IP " + DSIPaddr + " registered for collaboration.");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            // delete data
            dbc.deleteDSregistration(DSIPaddr);
            System.out.println("DS with IP " + DSIPaddr + " stopped collaborating.");

            // send confirm
            sendConfirm("nctr allow");
        }
        else {
            // neither start nor stop, error
        }
        sendDINF();
    }

    private void handleDINF(DatagramPacket receivePacket) {

        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();
        LinkedList<String> updatedNetIDs = new LinkedList<>();

        // get IP of DS that is sending the update
        String DSIP = receivePacket.getAddress().getHostAddress();

        // get list of NetIDs from packet
        for (int i = 1; i < messageParts.length; i++) {
            updatedNetIDs.add(messageParts[i].trim());
        }
        // apply DB updates
        dbc.updateRegisteredNSbyDS(DSIP, updatedNetIDs);
    }

    private void handleDDAT(DatagramPacket receivePacket) {
        DatabaseConnector dbc = new DatabaseConnector();

        // extract NetID
        String NetID = receivePacket.getData().toString().split(" ")[1];
        // lookup NetID in database
        String IP = dbc.lookupNSIPaddr(NetID);
        // forward message to matched IP
        try {
            DatagramSocket sendSocket = new DatagramSocket(SENDPORT);
            DatagramPacket sendPacket = new DatagramPacket(receivePacket.getData(), receivePacket.getLength(), InetAddress.getByAddress(IP.getBytes()), 6665);
            sendSocket.send(sendPacket);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendConfirm(String message) {
        try {
            DatagramSocket sendSocket = new DatagramSocket(SENDPORT);
            byte[] buf = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, receivePacket.getAddress(), receivePacket.getPort());
            sendSocket.send(sendPacket);
            sendSocket.close();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendDINF() {
        // get list of NetIDs served by this DS
        // get list of collaborating DSs
        // send NetID list to every DS in DINF format
    }
}
