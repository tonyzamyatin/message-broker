package mb.election.strategies;

import mb.election.ElectionContext;

import static mb.utils.CommandBuilder.*;

public class RaftElection extends BaseElection {

    private long votesReceived;
    private int voteId;
    private final Object voteLock = new Object();

    public RaftElection(ElectionContext context) {
        super(context);
        votesReceived = 0;
        voteId = -1;
    }

    @Override
    public void initiateHook() {
        synchronized (voteLock) {
            if (voteId != -1) {
                // Prevent initiating if already voted this term or election passed to new term
                return;
            }
            if (vote(context.selfId())) {
                votesReceived++;    // vote for self -> tie-breaker in edge case of only two nodes
            }
            votesReceived += context.sortedPeers().parallelStream()
                    .filter(peer -> peer.sendElectExpectVote(context.selfId(), context.responseTimeoutMs()))
                    .count();
            if (votesReceived > context.sortedPeers().size() / 2) {
                context.sortedPeers().parallelStream()
                        .forEach(peer -> peer.sendDeclareExpectAck(context.selfId(), context.responseTimeoutMs()));
                becomeLeader();
            }
        }
    }

    @Override
    public String electHook(int candidateId) {
        synchronized (voteLock) {
            vote(candidateId);
            return voteCmd(context.selfId(), voteId);
        }
    }

    @Override
    public String declareHook(int leaderId) {
        synchronized (voteLock) {
            voteId = -1;
            votesReceived = 0;
        }
        return ackCmd(context.selfId());
    }

    private boolean vote(int candidateId) {
        if (voteId == -1) {
            voteId = candidateId;
            return true;
        }
        return false;
    }
}
