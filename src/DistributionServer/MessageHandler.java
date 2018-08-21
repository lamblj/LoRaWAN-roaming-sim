package DistributionServer;

import java.io.IOException;
import java.net.*;

public class MessageHandler implements Runnable {

    private DatagramPacket receivePacket;

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

    private void handleNDAT(DatagramPacket receivePacket) {
        DatabaseConnector dbc = new DatabaseConnector();

        // extract NetID
        String NetID = receivePacket.getData().toString().split(" ")[1];
        // lookup NetID in database
        String IP = dbc.lookupNSIPaddr(NetID);
        // forward message to matched IP
        try {
            DatagramSocket sendSocket = new DatagramSocket();
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

    private void handleNCTR(DatagramPacket receivePacket) {
        // check if start or stop
        String[] messageParts = new String(receivePacket.getData()).split(" ");
        DatabaseConnector dbc = new DatabaseConnector();

        String cmd = messageParts[1].trim();
        String NetID = messageParts[2].trim();
        if (cmd.equals("start")){
            String NSIPaddr = receivePacket.getAddress().toString().substring(1);
            dbc.saveNSregistration(NetID, NSIPaddr);
            System.out.println("NS with ID " + NetID + ", and IP address " + NSIPaddr + " registered for roaming.");

            // send confirm
            try {
                DatagramSocket sendSocket = new DatagramSocket();
                byte[] buf = "nctr allow".getBytes();
                DatagramPacket sendPacket = new DatagramPacket(buf, buf.length, receivePacket.getAddress(), receivePacket.getPort());
                sendSocket.send(sendPacket);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else if (cmd.equals("stop")) {
            dbc.deleteNSregistration(NetID);
            System.out.println("NS with ID " + NetID + " stopped roaming.");
            // todo: send confirm
        }
        else {
            // neither start nor stop, error
        }

    }

    private void handleDDAT(DatagramPacket receivePacket) {
        System.out.println("Data from DS");
        // extract NetID
        // lookup NetID in database
        // forward message to matched IP
    }

    private void handleDCTR(DatagramPacket receivePacket) {
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

    private void handleDINF(DatagramPacket receivePacket) {
        System.out.println("Info from DS");
        // get IP of DS that is sending the update
        // get list of IPs from update
        // compare to currently stored IPs from that DS
        // make necessary changes

    }

}
