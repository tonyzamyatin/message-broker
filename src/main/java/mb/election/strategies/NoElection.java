package mb.election.strategies;

import mb.election.ElectionContext;
import mb.enums.ElectionState;

public class NoElection extends BaseElection {

    /**
     * Instantiates the election strategy and sets the election state to {@link ElectionState#FOLLOWER FOLLOWER}.
     */
    public NoElection(ElectionContext context) {
        super(context);
        becomeLeader();
    }

    @Override
    public String electHook(int candidateId) {
        // no implementation
        return null;
    }

    @Override
    public String declareHook(int leaderId) {
        // no implementation
        return null;
    }

    @Override
    protected void initiateHook() {
        // no implementation
    }
}
