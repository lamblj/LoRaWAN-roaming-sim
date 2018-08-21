package EndDevices;

import java.net.UnknownHostException;
import java.util.Scanner;

public class EndDeviceMain {

    public static void main(String[] args) throws UnknownHostException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Write the amount of devices to simulate: ");
        int amount = sc.nextInt();
        System.out.println("Write the ip address of the Gateway");
        String ipAddr = sc.next();
        System.out.println("Write the port of the Gatway");
        int port = sc.nextInt();
        System.out.println("Write how many of the devices will be roaming in percent:  ");
        int percentageRoaming = sc.nextInt();
        System.out.println("Write the network ID of non-roaming devices (12 Symbols)");
        String networkID = sc.next();

        EndDevices devices = new EndDevices(amount,ipAddr, port, percentageRoaming,networkID);
        devices.Initialize();

    }
}
