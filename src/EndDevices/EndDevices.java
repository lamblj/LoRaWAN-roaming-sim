package EndDevices;

import java.io.IOException;
import java.net.*;
import java.util.*;

public class EndDevices {
    private int amount;
    private int percentRoaming;
    private Random rando = new Random();
    private final int BUFSIZE = 1024;
    private InetAddress address;
    private int port =4445;
    private byte[] buf = new byte[BUFSIZE];
    private ArrayList<Message> devices = new ArrayList<Message>();
    private String networkId;
    private String roamingNetworkID;
    private final double datarate=31.25;
    public EndDevices(int amount, String ip, int percentRoaming, String networkId, String roamingNetworkID) throws UnknownHostException {
        this.amount = amount;
        this.percentRoaming = percentRoaming;
        this.address = InetAddress.getByName(ip);
        this.networkId = networkId;
        this.roamingNetworkID = roamingNetworkID;
    }


    public void Initialize() {

        int nonRoaming = (amount * percentRoaming) / 100;
       for (int i = 0; i < nonRoaming; i++) {
        devices.add(GenerateNonRoamingMSG(networkId));

        }

       for (int i = 0; i < amount - nonRoaming; i++) {
            devices.add(GenerateMessage(roamingNetworkID));
        }
        Collections.shuffle(devices);


       for (int i = 0; i < devices.size(); i++) {
           new MessageHandler(devices.get(i)).run();
       }


    }

    class MessageHandler extends TimerTask {
        private Message msg;

        public MessageHandler(Message msg) {
            this.msg = msg;
            this.msg = RefreshMessage(msg);
        }


        @Override
        public void run() {
            Timer timer = new Timer();
            if (msg.getCurrentMessages() >= msg.getMessageLimit()) {
                System.out.println("Hard limit of messages reached");
                Thread.currentThread().interrupt();
            } else {
                SendData(msg);

                int msglength =  msg.getData().length();
                double delay = msglength/datarate ;

                timer.schedule(new MessageHandler(this.msg), ((Math.round(delay) * 100) + new Random().nextInt(25)) * 1000);
                Thread.currentThread().interrupt();

            }
        }


        private void SendData(Message msg) {
            DatagramSocket socket;
            try {
                socket = new DatagramSocket();
                System.out.println(msg.getNetID() + msg.getData());
                buf = msg.MessageToBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
                msg.setCurrentMessages(msg.getCurrentMessages() + 1);
                msg = RefreshMessage(msg);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }


    private Message GenerateNonRoamingMSG(String netID) {

        Message msg = new Message(netID, createText(1+ rando.nextInt(58)));
        return msg;
    }

    private Message GenerateMessage(String roamingNetworkID) {
        String netID = roamingNetworkID;

        Message message = new Message(netID, createText(1+ rando.nextInt(58)));
        return message;
    }

    private Message RefreshMessage(Message message) {
        message.setData(createText( 1+ rando.nextInt(58)));
        return message;


    }

    private String createText(int length) {
        StringBuffer sb = new StringBuffer();
        while (sb.length() < length) {
            sb.append(Integer.toHexString(rando.nextInt(16)));
        }
        String content = sb.toString().substring(0, sb.length() - 1);
        return content;
    }
}










