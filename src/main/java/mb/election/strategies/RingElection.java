package mb.election.strategies;

import mb.election.Peer;
import mb.election.ElectionContext;

import java.util.List;

import static mb.election.strategies.ElectionUtils.getSuccessorsInRing;
import static mb.utils.CommandBuilder.*;

public class RingElection extends BaseElection {
    private final List<Peer> successors;

    public RingElection(ElectionContext context) {
        super(context);
        successors = getSuccessorsInRing(context.sortedPeers(), context.selfId());
    }

    @Override
    public void initiateHook() {
        boolean anyPeerResponded = elect(context.selfId());
        if (!anyPeerResponded) {
            onElectionWin();
        }
    }

    @Override
    public String electHook(int receivedId) {
        if (receivedId == context.selfId()) {
            // Elect message traveled around the entire ring and this node has the highest ID => election win!
            onElectionWin();
        } else {
            // Propagate the higher of the two IDs
            int candidateId = Math.max(context.selfId(), receivedId);
            boolean anyPeerResponded = elect(candidateId);
            if (!anyPeerResponded) {
                onElectionWin();
            }
        }
        return OK;
    }

    @Override
    public String declareHook(int leaderId) {
        if (leaderId != context.selfId()) {
            // Only forward declare if it's another node id, otherwise this node is leader and the message
            // travelled around the entire ring already
            declare(leaderId);
        }
        return ackCmd(context.selfId());
    }

    private void onElectionWin() {
        declare(context.selfId());
        becomeLeader();
    }

    private boolean elect(int candidateId) {
        return successors.stream().anyMatch(successor -> successor.sendElectExpectOk(candidateId, context.responseTimeoutMs()));
    }

    private boolean declare(int leaderId) {
        return successors.stream().anyMatch(successor -> successor.sendDeclareExpectAck(leaderId, context.responseTimeoutMs()));
    }
}
