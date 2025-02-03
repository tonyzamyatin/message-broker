package mb.broker.exchange;

import mb.broker.exchange.trie.Trie;
import mb.enums.ExchangeType;

import java.util.List;
import java.util.concurrent.BlockingQueue;

public class TopicExchange extends IExchange {
    private final Trie boundQueuesTrie = new Trie();
    @Override
    public ExchangeType getType() {
        return ExchangeType.TOPIC;
    }

    @Override
    void bindQueueHook(String bindingKey, BlockingQueue<String> queue) {
        boundQueuesTrie.insert(bindingKey, queue);
    }

    @Override
    public void publishMsgHook(String routingKey, String message) {
        List<BlockingQueue<String>> targetQueues = boundQueuesTrie.search(routingKey);
        targetQueues.forEach(queue -> {
            try {
                queue.put(message);
            } catch (InterruptedException e) {
                System.err.printf("Server error: " +
                        "Failed to put message '%s' into queue of fanout exchange\n", message);
            }
        });
    }

}
