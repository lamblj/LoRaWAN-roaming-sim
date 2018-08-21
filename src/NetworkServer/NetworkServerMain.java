package NetworkServer;


import java.util.Scanner;

public class NetworkServerMain {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        System.out.println("Write the ip address of the Distribution Server: ");
        String ipAddr = sc.next();
        System.out.println("Write the network ID of the Network Server (12 Symbols): ");
        String networkId = sc.next();
        NetworkServer ns = new NetworkServer(ipAddr, networkId);
        ns.Initialize();
    }
}

