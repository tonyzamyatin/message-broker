package mb.election.strategies;

import mb.election.Peer;
import mb.election.ElectionContext;

import java.util.List;

import static mb.utils.CommandBuilder.OK;
import static mb.utils.CommandBuilder.ackCmd;

public class BullyElection extends BaseElection {
    private final List<Peer> higherIdPeers;

    public BullyElection(ElectionContext context) {
        super(context);
        higherIdPeers = ElectionUtils.getHigherIdPeers(context.sortedPeers(), context.selfId());
    }

    @Override
    public void initiateHook() {
        boolean anyHigherIdPeerResponded = higherIdPeers.parallelStream()
                .anyMatch(peer -> peer.sendElectExpectOk(context.selfId(), context.responseTimeoutMs()));
        if (!anyHigherIdPeerResponded) {
            // Assume this node to be the highest ID node in the cluster and declare it as leader
            context.sortedPeers().parallelStream()
                    .forEach(peer -> peer.sendDeclareExpectAck(context.selfId(), context.responseTimeoutMs()));
            becomeLeader();
        }
    }

    @Override
    public String electHook(int candidateId) {
        // Handle election request by taking over election if own ID is higher than candidate's ID
        if (context.selfId() > candidateId) {
            this.initiate();
        }
        return OK;
    }

    @Override
    public String declareHook(int leaderId) {
        return ackCmd(context.selfId());
    }

}
