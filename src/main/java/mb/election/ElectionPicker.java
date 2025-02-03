package mb.election;

import mb.election.strategies.*;
import mb.enums.ElectionType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ElectionPicker {
    private static final Map<ElectionType, Function<ElectionContext, IElection>> registry = new HashMap<>();

    static {
        registry.put(ElectionType.RING, RingElection::new);
        registry.put(ElectionType.BULLY, BullyElection::new);
        registry.put(ElectionType.RAFT, RaftElection::new);
    }

    public static IElection getElectionStrategy(ElectionType electionType, ElectionContext context) {
        return registry.getOrDefault(electionType, NoElection::new).apply(context);
    }
}


