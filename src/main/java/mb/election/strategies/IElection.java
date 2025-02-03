package mb.election.strategies;

import mb.election.HeartbeatService;

public interface IElection {
    void initiate();

    String onElect(int candidateId, HeartbeatService heartbeatService);

    String onDeclare(int leaderId, HeartbeatService heartbeatService);

    int getLeader();
}
