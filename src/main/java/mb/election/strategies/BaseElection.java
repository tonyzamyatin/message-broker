package mb.election.strategies;

import mb.election.ElectionContext;
import mb.election.HeartbeatService;
import mb.enums.ElectionState;

public abstract class BaseElection implements IElection {

    protected ElectionContext context;
    protected volatile ElectionState electionState;
    protected volatile int leaderId;

    /**
     * Instantiates the election strategy and sets the election state to {@link ElectionState#FOLLOWER FOLLOWER}.
     */
    protected BaseElection(ElectionContext context) {
        this.context = context;
        electionState = ElectionState.FOLLOWER;
        leaderId = -1;
    }

    @Override
    public int getLeader() {
        return leaderId;
    }

    @Override
    public void initiate() {
        resetLeader();
        becomeCandidate();
        initiateHook();
    }

    @Override
    public String onElect(int receivedId, HeartbeatService heartbeatService) {
        heartbeatService.heartbeatReceived();
        resetLeader();
        becomeCandidate();
        return electHook(receivedId);
    }

    @Override
    public String onDeclare(int leaderId, HeartbeatService heartbeatService) {
        heartbeatService.heartbeatReceived();
        if (leaderId != context.selfId()) {
            updateLeader(leaderId);
            becomeFollower();
        }
        return declareHook(leaderId);
    }

    protected abstract String electHook(int receivedId);

    protected abstract String declareHook(int leaderId);

    protected abstract void initiateHook();

    protected void becomeCandidate() {
        electionState = ElectionState.CANDIDATE;
    }

    protected void becomeFollower() {
        electionState = ElectionState.FOLLOWER;
    }

    protected void becomeLeader() {
        updateLeader(context.selfId());
        electionState = ElectionState.LEADER;
        context.leaderCallback().run();
    }

    protected void resetLeader() {
        leaderId = -1;
    }

    protected void updateLeader(int newLeaderId) {
        leaderId = newLeaderId;
    }
}
