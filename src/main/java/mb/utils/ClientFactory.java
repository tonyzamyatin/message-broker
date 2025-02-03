package mb.utils;

import java.io.IOException;
import java.net.Socket;

public class ClientFactory {
    public static Client createClient(String host, int port) throws IOException {
        return new Client(new Socket(host, port));
    }
}
