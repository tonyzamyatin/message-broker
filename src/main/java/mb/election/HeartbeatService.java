package mb.election;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatService {
    private volatile ScheduledExecutorService heartbeatScheduler;
    private volatile long lastHeartbeatTimestamp;
    private final boolean enabled;

    /**
     * Create an instance of the heartbeat service.
     *
     * @param enabled whether the heartbeat service should be enabled at all. If false, any function calls to this
     *                heartbeat service will simply return without doing anything.
     */
    public HeartbeatService(boolean enabled) {
        this.lastHeartbeatTimestamp = System.currentTimeMillis(); // Initialize with the current time
        this.heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
        this.enabled = enabled;
    }

    /**
     * Periodically checks whether {@link HeartbeatService#lastHeartbeatTimestamp} exceeds the timeout.
     * Stops previously any started scheduling task first.
     *
     * @param onMissedHeartbeat  The callback function to run if no heartbeat is received within the timeout.
     * @param heartbeatTimeoutMs The maximal timeout between heartbeats (in milliseconds).
     */
    public void startMonitoring(Runnable onMissedHeartbeat, long heartbeatTimeoutMs) {
        if (!enabled) return;
        stop(); // Shutdown existing executor
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(); // Recreate executor
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() - lastHeartbeatTimestamp > heartbeatTimeoutMs) {
                onMissedHeartbeat.run();
            }
        }, 0, heartbeatTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Periodically schedules a heartbeat.
     * Stops previously any started scheduling task first.
     *
     * @param onHeartbeat        The action to run when a heartbeat occurs.
     * @param heartbeatTimeoutMs The time after which heartbeat repeat (in millisecond).
     * @param initialDelay       The time to wait before scheduling the action at a fixed rate.
     */
    public void startScheduling(Runnable onHeartbeat, long heartbeatTimeoutMs, long initialDelay) {
        if (!enabled) return;
        stop(); // Shutdown existing executor
        heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(); // Recreate executor
        heartbeatScheduler.scheduleAtFixedRate(onHeartbeat, initialDelay, heartbeatTimeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the heartbeat monitor, shutting down the scheduled task.
     */
    public void stop() {
        if (!enabled) return;
        if (!heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdown(); // Graceful shutdown
            try {
                if (!heartbeatScheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    heartbeatScheduler.shutdownNow(); // Force shutdown
                }
            } catch (InterruptedException e) {
                heartbeatScheduler.shutdownNow(); // Re-interrupt current thread
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Updates the last heartbeat timestamp to the current time.
     */
    public void heartbeatReceived() {
        if (!enabled) return;
        lastHeartbeatTimestamp = System.currentTimeMillis();
    }
}
