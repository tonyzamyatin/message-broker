package mb.broker.exchange;

import mb.enums.ExchangeType;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class FanoutExchange extends IExchange {
    private final CopyOnWriteArrayList<BlockingQueue<String>> boundQueues = new CopyOnWriteArrayList<>();

    @Override
    public ExchangeType getType() {
        return ExchangeType.FANOUT;
    }

    @Override
    void bindQueueHook(String bindingKey, BlockingQueue<String> queue) {
        boundQueues.add(queue);
    }

    @Override
    public void publishMsgHook(String routingKey, String message) {
        boundQueues.forEach(queue -> {
            try {
                queue.put(message);
            } catch (InterruptedException e) {
                System.err.printf("Server error: " +
                        "Failed to put message '%s' into queue of fanout exchange\n", message);
            }
        });

    }
}
