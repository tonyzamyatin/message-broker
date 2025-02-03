package mb.lifecycle;

/**
 * Interface-implementations manage the full server life cycle, including initialization, client
 * connection handling, and server shutdown. Optionally, it can also register domains with a specified DNS server.
 */
public interface IServerLifecycleManager {
    /**
     * Activates the server, initializing resources and starting to handle client requests.
     * This may include registering with a DNS server and accepting client connections.
     */
    void run();

    /**
     * Shuts down the server, releasing all allocated resources and terminating ongoing tasks.
     */
    void shutdown();
}
