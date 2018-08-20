package EndDevices;

public class EndDeviceMain {

    public static void main(String[] args) {

        EndDevices devices = new EndDevices(50);
        try {
            devices.Initialize(50, 40);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
