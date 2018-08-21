package EndDevices;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class EndDevices {
    private int amount;
    private int percentRoaming;
    private Random rando = new Random();
    private byte[] buf;
    private InetAddress address;
    private int port;
    private final int BUFSIZE = 1024;
    private ArrayList<Message> devices = new ArrayList<Message>();
    public EndDevices(int amount, String ip, int port, int percentRoaming) throws UnknownHostException {
        this.amount = amount;
        this.percentRoaming = percentRoaming;
        this.address = InetAddress.getByName(ip);
        this.port = port;
    }


    public void Initialize() {

        int nonRoaming = (amount * percentRoaming) / 100;
        for (int i = 0; i < nonRoaming; i++) {
            String text = createText(13);
            devices.add(GenerateNonRoamingMSG(text));

        }
        for (int i = 0; i < amount - nonRoaming; i++) {
            devices.add(GenerateMessage());
        }

        for(int i=0;i<amount;i++){
        new MessageHandler(devices.get(i)).run();
        }
    }

    class MessageHandler extends TimerTask {
    private Message msg ;

        public MessageHandler(Message msg) {
            this.msg = msg;
            SendData(msg);
            this.msg = RefreshMessage(msg);
        }


        @Override
        public void run() {
            Timer timer = new Timer();
            if(msg.getCurrentMessages() >= msg.getMessageLimit()){

            }
            else {
                timer.schedule(new MessageHandler(this.msg), 240 + new Random().nextInt(2800) * 1000);
                System.out.println(msg.getNetID() + " Packet  " + msg.getCurrentMessages());
                System.out.println("Messages sent: " + msg.getCurrentMessages());
                Thread.currentThread().interrupt();

            }
        }


        private void SendData(Message msg){
            DatagramSocket socket;
            try {
                socket = new DatagramSocket();
                buf = msg.MessageToBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
                socket.send(packet);
                msg.setCurrentMessages(msg.getCurrentMessages()+1);
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }



    private Message GenerateNonRoamingMSG(String netID) {

        Message msg = new Message(netID, createText(13 + rando.nextInt(55)));

        return msg;
    }

    private Message GenerateMessage() {
        String netID = createText(13);

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
            sb.append(Integer.toHexString(rando.nextInt(16)));
        }
        String content = sb.toString().substring(0, sb.length() - 1);
        return content;
    }
}










