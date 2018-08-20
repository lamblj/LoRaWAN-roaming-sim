package EndDevices;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;

import Message.Message;

public class EndDevices {
    private int amount;
    private int percentRoaming;
    private Random rando = new Random();
    private byte[] buf;
    private InetAddress address;
    private ArrayList<Message> devices = new ArrayList<Message>();
    private final int BUFSIZE = 1024;
    public EndDevices(int amount) {
        this.amount = amount;
        this.percentRoaming = 40;

    }

// TODO Timer functions and multiple message sending


    private void SendData() throws InterruptedException {
        DatagramSocket socket;
        try {
            socket = new DatagramSocket();
            address = InetAddress.getByName("localhost");
            Message msg = GenerateMessage();
            buf = msg.MessageToBytes();
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, 4445);
           // System.out.println("Your message is : " + msg.getData() + " Network ID is : " + msg.getNetID());
            System.out.println("Packet 1 " + new String(packet.getData()));
            socket.send(packet);
            msg = RefreshMessage(msg);
           // System.out.println("Your message is : " + msg.getData() + " Network ID is : " + msg.getNetID());
            buf = msg.MessageToBytes();
            packet.setData(buf);
            System.out.println("Packet 1 " + new String(packet.getData()));
            Thread.sleep(200);
            socket.send(packet);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void Initialize(int amount, int percentRoaming) throws InterruptedException {


        SendData();
    }

    private Message GenerateMessage() {
        String netID = createText(12);
        Message message = new Message(netID, createText(13 + rando.nextInt(55)));
        return message;
    }


    private Message RefreshMessage(Message message) {
        message.setData(createText(13 + rando.nextInt(55)));
        return message;


    }


    private String createText(int length) {
        StringBuffer sb = new StringBuffer();
        while (sb.length() < length) {
            sb.append(Integer.toHexString(rando.nextInt()));
        }
        String content = sb.toString().substring(0, sb.length() - 1);
        return content;
    }

    // To Do
    private boolean TimeController() {

        return false;
    }
}



