package mb;

/**
 * Base interface for all server implementations.
 * Provides a standard structure for server startup and shutdown behavior.
 */
public interface IServer extends Runnable {

    /**
     * Shuts down the server and releases any allocated resources.
     */
    void shutdown();
}
