package mb.utils;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import static mb.utils.LoggingUtil.logErrorMsg;

public class Client implements AutoCloseable {
    public final Socket socket;
    public IOUtils io;

    public Client(Socket socket) throws IOException {
        this.socket = socket;
        io = new IOUtils(socket.getInputStream(), socket.getOutputStream());
    }

    public String host() {
        return socket.getInetAddress().getHostAddress();
    }

    public int port() {
        return socket.getPort();
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close(); // This will automatically shutdown associated streams.
        }
    }

    /**
     * Attempt to send a message and wait for a response.
     *
     * @param timeoutMs        The time to wait for the response
     * @return the response that was received within the timeout, otherwise null.
     */
    public String waitForResponse(int timeoutMs) {
        try {
            int originalTimout = socket.getSoTimeout();
            try {
                socket.setSoTimeout(timeoutMs);
                return io.readMessage();

            } catch (IOException e) {
                System.err.printf("Error or timeout while waiting for response: %s%n", e.getMessage());
            } finally {
                try {
                    // Reset the timeout after the operation
                    socket.setSoTimeout(originalTimout);
                } catch (SocketException e) {
                    System.err.printf("Error resetting socket timeout: %s%n", e.getMessage());
                }
            }
        } catch (SocketException e) {
            logErrorMsg(e, "Unable to update socket timeout: %s", e.getMessage());
        }
        return null;
    }
}
