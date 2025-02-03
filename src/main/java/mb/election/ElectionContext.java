package mb.election;

import java.util.List;

/**
 * The election context.
 *
 * @param selfId            id of this node in the cluster
 * @param sortedPeers       list of known peers sorted by id in ascending order.
 * @param responseTimeoutMs timeout when waiting for peer response to election request (in milliseconds)
 * @param leaderCallback    callback function to call when this node becomes the leader.
 */
public record ElectionContext(
        int selfId,
        int responseTimeoutMs,
        Runnable leaderCallback,
        List<Peer> sortedPeers
) {
}
