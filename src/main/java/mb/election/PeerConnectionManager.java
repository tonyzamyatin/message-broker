package mb.election;

import mb.utils.Client;

import java.io.IOException;
import java.net.*;
import java.util.concurrent.*;

import static mb.utils.LoggingUtil.*;

/**
 * Manages peer connections in a distributed cluster. This class is responsible for establishing and maintaining
 * connections with known peers, accepting incoming connections, and facilitating communication between peers.
 */
public class PeerConnectionManager {
    private final ServerSocket peerConnectionSocket;
    private final IPeerConnectionHandler peerConnectionHandler;
    private final ExecutorService peerConnectionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Constructs a PeerConnectionManager with a given server socket and a list of known peers.
     *
     * @param socket                the server socket used for accepting incoming connections.
     * @param peerConnectionHandler the handler to process messages received from peers.
     */
    public PeerConnectionManager(ServerSocket socket, IPeerConnectionHandler peerConnectionHandler) {
        this.peerConnectionSocket = socket;
        this.peerConnectionHandler = peerConnectionHandler;
    }

    /**
     * Gracefully shuts down the PeerConnectionManager by closing the server socket and shutting down
     * the executor service.
     */
    public void shutdown() {
        try {
            if (peerConnectionSocket != null && !peerConnectionSocket.isClosed()) {
                peerConnectionSocket.close();
            }
        } catch (SocketException ignored) { // Occurs when serverSocket.accept() is interrupted by serverSocket.shutdown()
        } catch (IOException e) {
            throw new RuntimeException("Error closing peer connection socket", e);
        }
        peerConnectionExecutor.shutdown();
        try {
            boolean allTerminated = peerConnectionExecutor.awaitTermination(200, TimeUnit.MILLISECONDS);
            if (!allTerminated) {
                peerConnectionExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            peerConnectionExecutor.shutdownNow();   // (Re-)Cancel if current thread also interrupted
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }

    /**
     * Joins the cluster by and start accepting incoming connections from peers.
     */
    public void joinCluster() {
        peerConnectionExecutor.execute(this::acceptPeerConnections);
    }

    /**
     * Continuously accepts incoming peer connections and stores them in the active connections map. If an existing connection
     * already exists, it replaces the old connection with the new one.
     */
    private void acceptPeerConnections() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Client connection = new Client(peerConnectionSocket.accept());
                peerConnectionExecutor.execute(() -> {
                    connection.io.sendMessage("ok LEP");
                    try {
                        peerConnectionHandler.handle(connection);
                    } catch (IOException e) {
                        // logWarningMsg("Failed while handling messages from peer at %s:%d: %s", client.host(), client.port(), e.getMessage());
                    }
                });
            } catch (IOException e) {
                if (peerConnectionSocket.isClosed()) {
                    logInfoMsg("Server socket on port %d is closed. Stopping to accept peer connection.", peerConnectionSocket.getLocalPort());
                    break;
                }
                logWarningMsg("Failed to accept a peer connection: " + e.getMessage());
            } catch (RejectedExecutionException ignored) {}
        }
    }
}
