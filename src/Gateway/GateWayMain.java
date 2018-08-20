package Gateway;

import java.io.IOException;

public class GateWayMain {

    public static void main(String[] args) throws IOException {

        Gateway gw = new Gateway("127.0.0.1");
        gw.Initialize();


    }

}
