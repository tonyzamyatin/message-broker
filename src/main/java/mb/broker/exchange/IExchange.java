package mb.broker.exchange;

import mb.enums.ExchangeType;

import java.util.concurrent.BlockingQueue;

/**
 * Interface for SMQP exchanges
 */
public abstract class IExchange {
    /**
     * Get the type of the exchange
     *
     * @return the type of this exchange
     */
    public abstract ExchangeType getType();

    /**
     * Binds the queue to the exchange, if the binding key is not already in use. This operation is blocking.
     *
     * @param bindingKey the binding key to bind the queue to the exchange with (not null, not empty).
     *                   If a binding key is required by the exchange type, then the binding key must be at least one word.
     *                   If it consists of more than one word, they must be delimited by dots (`.`).
     *                   Additionally, pattern matching symbols may be used as the last word: `*` means exactly
     *                   one word, `#` means zero or more words.
     * @param queue      the queue to bind to the exchange (not null).
     */
    public final void bindQueue(String bindingKey, BlockingQueue<String> queue) {
        assert bindingKey != null && !bindingKey.isEmpty() : "Binding key is null or empty.";
        assert queue != null : "Queue is null.";
        bindQueueHook(bindingKey, queue);
    }

    /**
     * Publish a message to the exchange, if there exists a queue witch a matching binding key. If the routing key is
     * invalid or empty the message is silently ignored. This operation is blocking.
     *
     * @param routingKey the routing key of the message (not null)
     * @param message    the message to be published (not null, not empty)
     */
    public final void publish(String routingKey, String message) {
        assert routingKey != null : "Routing key is null.";
        assert message != null && !message.isEmpty() : "Message is null or empty.";
        if (routingKey.isBlank()) {
            return;
        }
        publishMsgHook(routingKey, message);
    }

    /**
     * Hook method to bind a queue to the exchange with the specified binding key, if it is not already in use.
     * Subclasses must implement this method to define their specific binding logic.
     *
     * @param bindingKey the binding key to bind the queue
     * @param queue      the queue to be bound
     */
    abstract void bindQueueHook(String bindingKey, BlockingQueue<String> queue);

    /**
     * Hook method to publish a message to the queue specified by the routing key.
     * Subclasses must implement this method to define their specific binding logic.
     *
     * @param routingKey the binding key to bind the queue
     * @param message    the message to be published
     */
    abstract void publishMsgHook(String routingKey, String message);
}

