package Gateway;


import java.util.Scanner;

public class GateWayMain {

    public static void main(String[] args)  {
        Scanner sc = new Scanner(System.in);
        System.out.println("Write the ip address of the Network Server: ");
        String ipAddr = sc.next();
        System.out.println("Write the port of the Network Server");
        int port = sc.nextInt();
        Gateway gw = new Gateway(ipAddr);
        gw.Initialize();


    }

}
