package mb.dns;

import mb.IServer;

/**
 * Defines the core functionality of a DNS server.
 * This interface extends `IServer` and provides methods for starting and shutting down the server.
 */
public interface IDNSServer extends IServer {

    /**
     * Starts the DNS server and begins processing incoming DNS requests.
     */
    @Override
    void run();

    /**
     * Shuts down the DNS server gracefully, ensuring that all active connections are handled properly.
     */
    @Override
    void shutdown();
}
