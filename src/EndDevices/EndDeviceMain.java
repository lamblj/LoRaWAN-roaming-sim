package EndDevices;

import java.net.UnknownHostException;

public class EndDeviceMain {

    public static void main(String[] args) throws UnknownHostException {

        EndDevices devices = new EndDevices(2, "127.0.0.1",4445,20);

            devices.Initialize();

    }
}
