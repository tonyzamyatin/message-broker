package mb.lifecycle;

import mb.config.DNSConfig;
import mb.utils.Client;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static mb.utils.ClientFactory.createClient;
import static mb.utils.LoggingUtil.logErrorMsg;

public class ServerLifecycleManager implements IServerLifecycleManager {

    private final ServerSocket clientConnectionSocket;
    private final IConnectionHandler clientConnectionHandler;
    private final ExecutorService clientProtocolExecutor = Executors.newVirtualThreadPerTaskExecutor();
    DNSConfig dnsConfig;

    // ==============CONSTRUCTORS==============
    public ServerLifecycleManager(int port, IConnectionHandler clientConnectionHandler) {
        try {
            clientConnectionSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to start server on port %d", port), e);
        }
        this.clientConnectionHandler = clientConnectionHandler;
    }

    public ServerLifecycleManager(int port, IConnectionHandler clientConnectionHandler, DNSConfig dnsConfig) {
        this(port, clientConnectionHandler);
        this.dnsConfig = dnsConfig;
    }

    // ==============INTERFACES==============
    public void run() {
        registerDomain(dnsConfig);  // TODO: Validate when what domain from the Broker Config is used
        acceptClientConnections(clientConnectionHandler);
    }

    public void shutdown() {
        try {
            if (clientConnectionSocket != null && !clientConnectionSocket.isClosed()) {
                clientConnectionSocket.close();
            }
        } catch (SocketException ignored) { // Occurs when serverSocket.accept() is interrupted by serverSocket.shutdown()
        } catch (IOException e) {
            throw new RuntimeException("Error closing client connection socket", e);
        }

        clientProtocolExecutor.shutdown();     // Prevents new tasks from being submitted
        try {
            boolean allTerminated = clientProtocolExecutor.awaitTermination(200, TimeUnit.MILLISECONDS);
            if (!allTerminated) {
                clientProtocolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            clientProtocolExecutor.shutdownNow();      // (Re-)Cancel if current thread also interrupted
            Thread.currentThread().interrupt(); // Preserve interrupt status
        }
    }

    // ==============IMPLEMENTATION==============
    private void registerDomain(DNSConfig dnsConfig) {
        if (dnsConfig == null) {
            return;
        }

        try (Client connection = createClient(dnsConfig.dnsHost(), dnsConfig.dnsPort())) {
            if (!"ok SDP".equals(connection.io.readMessage())) {
                logErrorMsg("Protocol error: expected greeting 'ok SDP' from DNS server, but received an unexpected response");
            }
            connection.io.sendMessage("register %s %s:%s"
                    .formatted(dnsConfig.domain(), dnsConfig.host(), dnsConfig.port()));
            if (!"ok".equals(connection.io.readMessage())) {
                logErrorMsg("DNS error: could not register domain name '%s' with DNS server at %s:%s"
                        .formatted(dnsConfig.domain(), dnsConfig.dnsHost(), dnsConfig.dnsPort()));
            }
        } catch (ConnectException ignored) {
        } catch (IOException e) {
            logErrorMsg(e, "Failed to register name with DNS: ", e.getMessage());
        }
    }

    private void acceptClientConnections(IConnectionHandler clientConnectionHandler) {
        try {
            while (!Thread.currentThread().isInterrupted() && !clientConnectionSocket.isClosed()) {
                Socket connection = clientConnectionSocket.accept();
                clientProtocolExecutor.execute(() -> {
                    // Defensive programming: Exceptions should be handled in worker threads, but handle in case worker
                    // implementation does throw an exception.
                    try {
                        clientConnectionHandler.handle(connection);
                    } catch (SocketException ignored) {
                    } catch (IOException e) {
                        logErrorMsg(e, "I/O exception in handler: " + e.getMessage());
                    } catch (RuntimeException e) {
                        logErrorMsg(e, "Runtime exception in handler: " + e.getMessage());
                    } catch (Throwable t) { // Catch anything else
                        logErrorMsg("Unexpected exception in handler: " + t.getMessage());
                    }
                });
            }
        } catch (IOException e) {
            if (!clientConnectionSocket.isClosed()) {
                throw new RuntimeException("Error accepting connections", e);
            }
        }
    }
}
