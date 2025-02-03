package mb.election;

import mb.utils.Client;

import java.io.IOException;

@FunctionalInterface
public interface IPeerConnectionHandler {
    void handle(Client connection) throws IOException;
}
