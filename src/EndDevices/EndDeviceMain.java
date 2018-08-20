package EndDevices;

public class EndDeviceMain {

    public static void main(String[] args) {

        EndDevices devices = new EndDevices(50);
        try {
            devices.Initialize();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
