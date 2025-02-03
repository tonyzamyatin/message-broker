package mb.election;

/**
 * Interface for managing the leader election process in a distributed system.
 * Provides methods to startMonitoring the election protocol, manage the lifecycle, and query election state.
 */
public interface IElectionManager {
    /**
     * Starts the Leader Election Protocol (LEP).
     * This initializes peer connections, starts monitoring heartbeats, and handle peer communication.
     * If this node wins a leader election, the server protocol is executed and heartbeats are sent to peers in
     * periodic intervals.
     * @param serverProtocol the server protocol to run when this node becomes leader.
     */
    void start(Runnable serverProtocol);

    /**
     * Shuts down the election manager, closing all active connections and stopping related tasks.
     */
    void shutdown();

    /**
     * Initiates an election process, allowing this node to propose itself as the leader.
     */
    void initiateElection();

    /**
     * Retrieves the ID of the current leader.
     *
     * @return the leader's ID, or -1 if no leader is elected.
     */
    int getLeader();

    /**
     * Retrieves the ID of this node in the election system.
     *
     * @return the election ID of the current node.
     */
    int getId();
}
