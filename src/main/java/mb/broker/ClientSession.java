package mb.broker;

public class ClientSession implements IClientSession {
    private Thread subscriptionThread;
    private String lastDeclaredExchange;
    private String lastDeclaredQueue;

    @Override
    public void setLastDeclaredExchange(String exchangeName) {
        lastDeclaredExchange = exchangeName;
    }

    @Override
    public String getLastDeclaredExchange() {
        return lastDeclaredExchange;
    }

    @Override
    public void setLastDeclaredQueue(String queueName) {
        lastDeclaredQueue = queueName;
    }

    @Override
    public String getLastDeclaredQueue() {
        return lastDeclaredQueue;
    }

    @Override
    public boolean startSubscription(Runnable subscriptionTask) {
        if (subscriptionThread != null) {
            return false;
        }
        subscriptionThread = Thread.startVirtualThread(subscriptionTask);
        return true;
    }

    @Override
    public void stopSubscription() {
        if (subscriptionThread != null) {
            subscriptionThread.interrupt();
            try {
                subscriptionThread.join(); // Ensure the thread finishes execution
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Restore interrupt status
            }
            subscriptionThread = null;
        }
    }

    @Override
    public void close() {
        stopSubscription();
    }
}
