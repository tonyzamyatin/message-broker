package mb.election;

import mb.election.strategies.IElection;
import mb.config.ElectionConfig;
import mb.enums.ElectionType;
import mb.utils.Client;
import mb.utils.IOUtils;
import mb.utils.ValidationUtils;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static mb.election.ElectionPicker.getElectionStrategy;
import static mb.utils.ClientFactory.createClient;
import static mb.utils.CommandBuilder.*;
import static mb.utils.ValidationUtils.isInt;

public class ElectionManager implements IElectionManager {
    private final ElectionConfig electionConfig;
    private final List<Peer> sortedPeers;
    private final IElection election;

    private final HeartbeatService heartbeatService;
    private final PeerConnectionManager peerConnectionManager;

    private boolean shutdown = false;


    // Latch to wait until this node becomes the leader
    CountDownLatch leaderLatch = new CountDownLatch(1);

    public ElectionManager(ElectionConfig electionConfig) {
        this.electionConfig = electionConfig;
        sortedPeers = IntStream.range(0, electionConfig.electionPeerIds().length)
                .mapToObj(i -> new Peer(
                        electionConfig.electionPeerIds()[i],
                        electionConfig.electionPeerHosts()[i],
                        electionConfig.electionPeerPorts()[i]))
                .sorted()   // Peer needs to implement Comparable
                .toList();
        election = getElectionStrategy(
                electionConfig.electionType(),
                new ElectionContext(getId(),50, this::onElectionWin, sortedPeers));
        heartbeatService = new HeartbeatService(electionConfig.electionType() != ElectionType.NONE);
        try {
            ServerSocket peerConnectionSocket = new ServerSocket(electionConfig.electionPort());
            peerConnectionManager = new PeerConnectionManager(peerConnectionSocket, this::handlePeerConnection);
        } catch (IOException e) {
            throw new RuntimeException(String.format(
                    "Failed to open server socket for leader election protocol on port %d: %s",
                    electionConfig.electionPort(), e.getMessage()));
        }
    }

    @Override
    public int getId() {
        return electionConfig.electionId();
    }

    @Override
    public int getLeader() {
        return election.getLeader();
    }

    @Override
    public void initiateElection() {
        election.initiate();
    }

    @Override
    public void start(Runnable serverProtocol) {
        peerConnectionManager.joinCluster();
        heartbeatService.startMonitoring(this::initiateElection, electionConfig.electionHeartbeatTimeoutMs());

        // Wait until this node becomes the leader
        try {
            leaderLatch.await();    // blocks until onElectionWin() is called
            // Start scheduling heartbeats and run the server protocol in the current thread
            if (!shutdown) {
                heartbeatService.startScheduling(this::sendPingToAllPeers, 20, 0);
                serverProtocol.run();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting to become the leader", e);
        }
    }

    @Override
    public void shutdown() {
        shutdown = true;
        if (leaderLatch.getCount() > 0) {
            leaderLatch.countDown();
        }
        peerConnectionManager.shutdown();
        heartbeatService.stop();
    }

    private void onElectionWin() {
        leaderLatch.countDown();  // Signal that this node is now the leader
    }

    private void sendPingToAllPeers() {
        sortedPeers.parallelStream()
                .forEach(peer -> {
                    try (Client connection = createClient(peer.host(), peer.port())) {
                        connection.io.sendMessage(PING);
                    } catch (IOException ignored) {
                    }
                });
    }

    private void handlePeerConnection(Client connection) throws IOException {
        while (!Thread.currentThread().isInterrupted() && !connection.socket.isClosed()) {
            String message = connection.io.readMessage();
            if (message == null) {
                // EOF reached by stream => connection corrupter or closed on other side
                // logInfoMsg("Peer at %s:%d closed the connection.", client.host(), client.port());
                connection.close();
                break;
            }
            if (message.isBlank()) {
                continue;
            }
            String[] args = message.strip().split(" ");
            String cmd = args[0];
            switch (cmd) {
                case PING -> handlePing(connection.io, args);
                case DECLARE -> handleDeclare(connection.io, args);
                case ELECT -> handleElect(connection.io, args);
                default -> connection.io.printError("protocol error");
            }
        }
    }

    private void handlePing(IOUtils io, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 1)) {
            io.printUsage(PING);
            return;
        }
        io.sendMessage(PONG);
        heartbeatService.heartbeatReceived();
    }

    private void handleDeclare(IOUtils io, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 2) || !isInt(args[1])) {
            io.printUsage(DECLARE + " <id>");
            return;
        }
        String res = election.onDeclare(Integer.parseInt(args[1]), heartbeatService);
        io.sendMessage(res);
    }

    private void handleElect(IOUtils io, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 2) || !isInt(args[1])) {
            io.printUsage(ELECT + " <id>");
            return;
        }
        String res = election.onElect(Integer.parseInt(args[1]), heartbeatService);
        io.sendMessage(res);
    }
}
