package mb.lifecycle;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
public interface IConnectionHandler {
    /**
     * Handle a socket connection.
     *
     * @param connection A client connection
     */
    void handle(Socket connection) throws IOException;
}
