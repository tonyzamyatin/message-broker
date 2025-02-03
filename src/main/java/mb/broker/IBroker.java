package mb.broker;

import mb.IServer;

/**
 * Defines the core functionality for a Message Broker server.
 * This interface extends `IServer` and includes additional methods
 * for leader election and broker identification.
 */
public interface IBroker extends IServer {

    /**
     * Starts the broker server.
     * This method contains the main logic for message processing and coordination.
     */
    @Override
    void run();

    /**
     * Returns the unique identifier assigned to this broker instance.
     * The ID is used for leader election and coordination between brokers.
     *
     * @return the broker's unique ID.
     */
    int getId();

    /**
     * Triggers an immediate leader election process.
     * This broker will participate in the election and attempt to determine
     * the leader among all connected peers.
     */
    void initiateElection();

    /**
     * Returns the ID of the currently elected leader.
     * If no leader has been elected yet, this method returns `-1`.
     *
     * @return the ID of the elected leader, or `-1` if none exists.
     */
    int getLeader();

    /**
     * Shuts down the broker server gracefully, ensuring that
     * all ongoing tasks are completed before termination.
     */
    @Override
    void shutdown();
}
