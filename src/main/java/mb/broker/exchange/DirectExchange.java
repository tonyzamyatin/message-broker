package mb.broker.exchange;

import mb.enums.ExchangeType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DirectExchange extends IExchange {
    private final ConcurrentMap<String, CopyOnWriteArrayList<BlockingQueue<String>>> boundQueues = new ConcurrentHashMap<>();
    @Override
    public ExchangeType getType() {
        return ExchangeType.DIRECT;
    }

    @Override
    void bindQueueHook(String bindingKey, BlockingQueue<String> queue) {
        boundQueues.compute(bindingKey, (key, existingQueues) -> {  // atomic
            if (existingQueues == null) {
                // No existing list, create a new one and add the queue
                CopyOnWriteArrayList<BlockingQueue<String>> newQueueList = new CopyOnWriteArrayList<>();
                newQueueList.add(queue);
                return newQueueList;
            } else {
                // Add the queue to the existing list
                existingQueues.add(queue);
                return existingQueues;
            }
        });
    }

    @Override
    public void publishMsgHook(String routingKey, String message) {
        CopyOnWriteArrayList<BlockingQueue<String>> queues = boundQueues.get(routingKey);
        if (queues == null) return;
        queues.forEach(queue -> {
            if (queue != null) {
                try {
                    queue.put(message);
                } catch (InterruptedException e) {
                    System.err.printf("Server error: " +
                            "Failed to put message '%s' into queue with routing key %s\n", message, routingKey);
                }
            }
        });
    }
}
