package mb.config;

import mb.enums.ElectionType;

import java.util.Arrays;
import java.util.Objects;

public record ElectionConfig(
        int electionId,
        ElectionType electionType,
        String host,
        int electionPort,
        String[] electionPeerHosts,
        int[] electionPeerPorts,
        int[] electionPeerIds,
        long electionHeartbeatTimeoutMs
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ElectionConfig that)) return false;

        return electionId == that.electionId &&
                electionPort == that.electionPort &&
                host == that.host &&
                electionHeartbeatTimeoutMs == that.electionHeartbeatTimeoutMs &&
                Objects.equals(electionType, that.electionType) &&
                Arrays.equals(electionPeerIds, that.electionPeerIds) &&
                Arrays.equals(electionPeerPorts, that.electionPeerPorts) &&
                Arrays.equals(electionPeerHosts, that.electionPeerHosts);
    }

    @Override
    public int hashCode() {
        int result = electionId;
        result = 31 * result + Objects.hashCode(electionType);
        result = 31 * result + Objects.hashCode(host);
        result = 31 * result + electionPort;
        result = 31 * result + Arrays.hashCode(electionPeerHosts);
        result = 31 * result + Arrays.hashCode(electionPeerPorts);
        result = 31 * result + Arrays.hashCode(electionPeerIds);
        result = 31 * result + Long.hashCode(electionHeartbeatTimeoutMs);
        return result;
    }
}
