package mb.broker;

/**
 * Interface for client session handling
 */
public interface IClientSession extends AutoCloseable {

    /**
     * Sets the name of the last declared exchange for the current session.
     *
     * @param exchangeName the name of the exchange
     */
    void setLastDeclaredExchange(String exchangeName);

    /**
     * Retrieves the name of the last declared exchange for the current session.
     *
     * @return the name of the last declared exchange or null if not set.
     */
    String getLastDeclaredExchange();

    /**
     * Sets the name of the last specified queue for the current session.
     *
     * @param queueName the name of the queue
     */
    void setLastDeclaredQueue(String queueName);

    /**
     * Retrieves the name of the last specified queue for the current session.
     *
     * @return the name of the last specified queue or null if not set.
     */
    String getLastDeclaredQueue();

    /**
     * Start a client subscription in the background, if no there is no running subscription.
     *
     * @param subscriptionTask the subscription task to startLEP in the background
     * @return true if there is no running subscription, false otherwise.
     */
    boolean startSubscription(Runnable subscriptionTask);

    /**
     * Stops the active subscription, if there is one.
     */
    void stopSubscription();

    @Override
    void close();
}

