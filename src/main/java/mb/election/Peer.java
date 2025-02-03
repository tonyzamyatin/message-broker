package mb.election;

import mb.utils.Client;

import java.io.IOException;
import java.util.Objects;

import static mb.utils.ClientFactory.createClient;
import static mb.utils.CommandBuilder.*;
import static mb.utils.LoggingUtil.logWarningMsg;
import static mb.utils.ValidationUtils.*;

public record Peer(
        int id,
        String host,
        int port
) implements Comparable<Peer> {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Peer peer)) return false;

        return id == peer.id && port == peer.port && Objects.equals(host, peer.host);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + Objects.hashCode(host);
        result = 31 * result + port;
        return result;
    }

    @Override
    public int compareTo(Peer o) {
        return id - o.id;
    }

    /**
     * Send elect to peer and wait him to respond with ok.
     *
     * @param candidateId the ID of the candidate to elect
     * @return true if the peer responds with ok within the given timeout, false otherwise
     */
    public boolean sendElectExpectOk(int candidateId, int timeoutMs) {
        return sendMessageExpectResStartsWith(electCmd(candidateId), OK, timeoutMs);
    }

    /**
     * Send declare to peer and wait him to acknowledge the leader.
     *
     * @param leaderId the ID of the leader to declare
     * @return true if the peer acknowledges the leader within the given timeout, false otherwise
     */
    public boolean sendDeclareExpectAck(int leaderId, int timeoutMs) {
        return sendMessageExpectResStartsWith(declareCmd(leaderId), ACK, timeoutMs);
    }

    /**
     * Send elect to peer and wait him to respond with a vote.
     *
     * @param candidateId the ID of the candidate to elect
     * @return true if the peer responds with a vote for the candidate within the given timeout, false otherwise.
     */
    public boolean sendElectExpectVote(int candidateId, int timeoutMs) {
        try (Client connection = createClient(host, port)) {
            if (!validateResponseEquals(connection, OK + " LEP", timeoutMs)) {
                logWarningMsg("Peer %d failed to respond with greeting", id);
            }
            connection.io.sendMessage(electCmd(candidateId));
            String res = connection.waitForResponse(timeoutMs);
            String[] args = res.strip().split(" ");
            if (invalidArgNum(args, 3) || !VOTE.equals(args[0]) || !isInt(args[1]) || !isInt(args[2])) {
                connection.io.printUsage(VOTE + " <sender-id> <candidate-id>");
                return false;
            }
            return candidateId == Integer.parseInt(args[2]);
        } catch (IOException e) {
            // logWarningMsg("Failed to connect to peer %d at %s:%d: %s", id, host, port, e.getMessage());
        }
        return false;
    }

    /**
     * Send message to peer and expect response.
     *
     * @param message   the message to send
     * @param timeoutMs the time to wait for the peer's response
     * @return the peer's response if it responded within the timeout, null otherwise.
     */
    private String sendMessageExpectRes(String message, int timeoutMs) {
        try (Client connection = createClient(host, port)) {
            if (!validateResponseEquals(connection, OK + " LEP", timeoutMs)) {
                logWarningMsg("Peer %d failed to respond with greeting", id);
            }
            connection.io.sendMessage(message);
            return connection.waitForResponse(timeoutMs);
        } catch (IOException e) {
            // logWarningMsg("Failed to connect to peer %d at %s:%d: %s", id, host, port, e.getMessage());
        }
        return null;
    }

    /**
     * Attempt to send message to peer.
     *
     * @param message       the message to send
     * @param expectedStart the string that the response it expected to start with
     * @return true if the peer responds with a message that starts with the expected string within the given timeout, false otherwise
     */
    private boolean sendMessageExpectResStartsWith(String message, String expectedStart, int timeoutMs) {
        String res = sendMessageExpectRes(message, timeoutMs);
        return res != null && res.startsWith(expectedStart);
    }
}
