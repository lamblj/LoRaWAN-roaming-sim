package EndDevices;

public class Message {

//The LoRa Alliance allocates a 24bits unique network identifier (NetID)

    private String netID;




    private int currentMessages;

    private int messageLimit = 130;
    private String data;

    public Message(String netID, String data) {
        this.netID = netID;
        this.data = data;
    }

    public int getCurrentMessages() {
        return currentMessages;
    }

    public void setCurrentMessages(int currentMessages) {
        this.currentMessages = currentMessages;
    }

    public int getMessageLimit() {
        return messageLimit;
    }

    public String getNetID() {
        return netID;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public byte[] MessageToBytes() {
        return (this.netID + this.data).getBytes();
    }

}
