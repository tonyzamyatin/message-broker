package mb.election.strategies;

import mb.election.Peer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ElectionUtils {
    /**
     * Finds the peers in order of succession for a ring-based election protocol, starting from the first peer with an ID
     * greater than the given selfId. Wraps around the sorted list to include all successors in a ring-like fashion.
     *
     * @param sortedPeers a list of peers sorted by their IDs
     * @param selfId      the ID of the current node
     * @return a list of peers in order of succession
     */
    public static List<Peer> getSuccessorsInRing(List<Peer> sortedPeers, int selfId) {
        // Binary search to find the first peer with a higher ID
        int index = Collections.binarySearch(sortedPeers, new Peer(selfId, null, 0));

        // Adjust index to point to the first peer with a higher ID
        index = (index < 0) ? -(index + 1) : index + 1;

        // Combine peers in ring order: [higher IDs] + [lower IDs]
        List<Peer> successors = new ArrayList<>(sortedPeers.subList(index % sortedPeers.size(), sortedPeers.size()));
        successors.addAll(sortedPeers.subList(0, index % sortedPeers.size()));

        return successors;
    }

    public static List<Peer> getHigherIdPeers(List<Peer> peers, int selfId) {
        return peers.stream().filter(peer -> peer.id() > selfId).toList();
    }
}
