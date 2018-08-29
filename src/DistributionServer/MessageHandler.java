package DistributionServer;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.List;

public class MessageHandler implements Runnable {

    private DatagramPacket receivePacket;
    private final int SENDPORT = 3002;
    private DatagramSocket sendSocket;

    public MessageHandler(DatagramPacket receivePacket) {
        this.receivePacket = receivePacket;
        try {
            sendSocket = new DatagramSocket(SENDPORT);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        String messagetype = new String(receivePacket.getData()).split(" ")[0].trim();
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
        sendSocket.close();
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
            System.out.println("DS with IP " + DSIPaddr + " registered for collaboration.");

            // send confirm
            sendConfirm("dctr allow");
        }
        // if it is a stop request
        else if (cmd.equals("stop")) {
            // delete data
            dbc.deleteDSregistration(DSIPaddr);
            System.out.println("DS with IP " + DSIPaddr + " stopped collaborating.");

            // send confirm
            sendConfirm("dctr allow");
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

    private void handleDDAT(DatagramPacket receivePacket) {
        DatabaseConnector dbc = new DatabaseConnector();

        // extract NetID
        String NetID = receivePacket.getData().toString().split(" ")[1];
        // lookup NetID in database
        String IP = dbc.lookupNSIPaddr(NetID);
        // forward message to matched IP
        try {
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

    private void sendDINF() {
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
