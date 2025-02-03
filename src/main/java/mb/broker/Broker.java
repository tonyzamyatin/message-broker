package mb.broker;

import mb.config.MonitoringClientConfig;
import mb.monitoring.MonitoringClient;
import mb.utils.ValidationUtils;
import mb.ComponentFactory;
import mb.utils.IOUtils;
import mb.election.ElectionManager;
import mb.lifecycle.ServerLifecycleManager;
import mb.broker.exchange.*;
import mb.config.BrokerConfig;
import mb.config.DNSConfig;
import mb.config.ElectionConfig;
import mb.enums.ElectionType;
import mb.enums.ExchangeType;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.*;

public class Broker implements IBroker {
    ServerLifecycleManager serverLifecycleManager;
    ElectionManager electionManager;
    MonitoringClient monitoringClient;

    ConcurrentMap<String, IExchange> exchangeMap = new ConcurrentHashMap<>();
    ConcurrentMap<String, BlockingQueue<String>> queueMap = new ConcurrentHashMap<>();

    public Broker(BrokerConfig config) {
        String domain = ElectionType.fromString(config.electionType()) == ElectionType.NONE ? config.domain() : config.electionDomain();

        DNSConfig dnsConfig = new DNSConfig(
                config.host(),
                config.port(),
                config.dnsHost(),
                config.dnsPort(),
                domain
        );
        ElectionConfig electionConfig = new ElectionConfig(
                config.electionId(),
                ElectionType.fromString(config.electionType()),
                config.host(),
                config.electionPort(),
                config.electionPeerHosts(),
                config.electionPeerPorts(),
                config.electionPeerIds(),
                config.electionHeartbeatTimeoutMs()
        );
        MonitoringClientConfig monitoringClientConfig = new MonitoringClientConfig(
                config.monitoringHost(),
                config.monitoringPort(),
                config.host(),
                config.port()
        );

        serverLifecycleManager = new ServerLifecycleManager(config.port(), this::handleSMQPConnection, dnsConfig);
        electionManager = new ElectionManager(electionConfig);
        monitoringClient = new MonitoringClient(monitoringClientConfig);
        exchangeMap.put("default", new DefaultExchange());
    }

    public static void main(String[] args) {
        ComponentFactory.createBroker(args[0]).run();
    }

    @Override
    public void run() {
        electionManager.start(serverLifecycleManager::run);
    }

    @Override
    public int getId() {
        return electionManager.getId();
    }

    @Override
    public void initiateElection() {
        electionManager.initiateElection();
    }

    @Override
    public int getLeader() {
        return electionManager.getLeader();
    }

    @Override
    public void shutdown() {
        serverLifecycleManager.shutdown();
        electionManager.shutdown();
        monitoringClient.shutdown();
    }

    private void handleSMQPConnection(Socket SMQPConnection) throws IOException {
        try (
                Socket connection = SMQPConnection;
                IOUtils io = new IOUtils(connection.getInputStream(), connection.getOutputStream());
                IClientSession clientSession = new ClientSession()
        ) {
            io.sendMessage("ok SMQP");
            String message;
            while (!connection.isClosed() && (message = io.readMessage()) != null) {
                if (message.isEmpty()) {
                    continue;
                }
                String[] args = message.split(" ");
                String cmd = args[0];
                switch (cmd) {
                    case "exchange":
                        handleExchangeDeclaration(io, clientSession, args);
                        break;
                    case "queue":
                        handleQueueDeclaration(io, clientSession, args);
                        break;
                    case "bind":
                        handleQueueBinding(io, clientSession, args);
                        break;
                    case "subscribe":
                        handleSubscription(io, clientSession);
                        break;
                    case "publish":
                        handlePublish(io, clientSession, args);
                        break;
                    case "exit":
                        io.sendMessage("ok bye");
                        return; // Exits loops; resources will be closed automatically by try-with-resources block
                    default:
                        io.printError("protocol error");
                }
            }
        }
    }

    private void handleExchangeDeclaration(IOUtils io, IClientSession clientSession, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 3)) {
            io.printUsage("exchange <type> <name>");
            return;
        }
        if (Arrays.stream(ExchangeType.values()).noneMatch(e -> e.name().equals(args[1].toUpperCase()))) {
            io.printError("unknown exchange type: " + args[1]);
            return;
        }

        ExchangeType exchangeType = ExchangeType.valueOf(args[1].toUpperCase());
        String exchangeName = args[2];

       exchangeMap.computeIfAbsent(exchangeName, (name) -> {
           IExchange newExchange = null;
           switch (exchangeType) {
               case ExchangeType.DIRECT -> newExchange = new DirectExchange();
               case ExchangeType.FANOUT -> newExchange = exchangeMap.put(name, new FanoutExchange());
               case ExchangeType.TOPIC -> newExchange = exchangeMap.put(name, new TopicExchange());
           }
           return newExchange;
       });

        if (exchangeMap.get(exchangeName) != null) {
            if (exchangeType != exchangeMap.get(exchangeName).getType()) {
                io.printError("exchange already exists with different type");
                return;
            }
        } else {
            switch (exchangeType) {
                case ExchangeType.DIRECT -> exchangeMap.put(exchangeName, new DirectExchange());
                case ExchangeType.FANOUT -> exchangeMap.put(exchangeName, new FanoutExchange());
                case ExchangeType.TOPIC -> exchangeMap.put(exchangeName, new TopicExchange());
            }
        }
        clientSession.setLastDeclaredExchange(exchangeName);
        io.sendMessage("ok");
    }

    private void handleQueueDeclaration(IOUtils io, IClientSession clientSession, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 2)) {
            io.printUsage("queue <name>");
            return;
        }
        String queueName = args[1];
        queueMap.computeIfAbsent(queueName, (name) -> {
            BlockingQueue<String> queue = new LinkedBlockingQueue<>();
            exchangeMap.get("default").bindQueue(name, queue);
            return queue;
        });
        clientSession.setLastDeclaredQueue(queueName);
        io.sendMessage("ok");
    }


    private void handleQueueBinding(IOUtils io, IClientSession clientSession, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 2)) {
            io.printUsage("bind <binding-key>");
            return;
        }

        String bindingKey = args[1];
        IExchange exchange = getValidatedExchange(clientSession, io);
        if (exchange == null) return;
        BlockingQueue<String> queue = getValidatedQueue(clientSession, io);
        if (queue == null) return;
        exchange.bindQueue(bindingKey, queue);
        io.sendMessage("ok");
    }

    private void handleSubscription(IOUtils io, IClientSession clientSession) throws IOException {
        BlockingQueue<String> queue = getValidatedQueue(clientSession, io);
        if (queue == null) return;

        Runnable subscriptionTask = () -> {
            try {
                while (true) {
                    String msg = queue.take();
                    io.sendMessage(msg);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        boolean subscriptionStarted = clientSession.startSubscription(subscriptionTask);
        if (!subscriptionStarted) { // defensive programming
            io.printError("could not startMonitoring subscription");
            System.err.println("ServerError: Did not startMonitoring subscription because of active subscription");
        }

        io.sendMessage("ok");
        while (!"stop".equals(io.readMessage()));
        clientSession.stopSubscription();
    }

    private void handlePublish(IOUtils io, IClientSession clientSession, String[] args) {
        if (ValidationUtils.invalidArgNum(args, 3)) {
            io.printUsage("publish <routing-key> <message>");
            return;
        }
        String routingKey = args[1];
        String msg = args[2];

        IExchange exchange = getValidatedExchange(clientSession, io);
        if (exchange == null) return;

        exchange.publish(routingKey, msg);
        io.sendMessage("ok");

        monitoringClient.sendMessage(routingKey);
    }

    private IExchange getValidatedExchange(IClientSession clientSession, IOUtils io) {
        String exchangeName = clientSession.getLastDeclaredExchange();
        if (exchangeName == null) {
            io.printError("no exchange declared");
            return null;
        }

        IExchange exchange = exchangeMap.get(exchangeName);
        if (exchange == null) {
            io.printError("exchange does not exist");
            System.err.printf("Server error: Client-declared exchange `%s` does not exist%n", exchangeName);
        }
        return exchange;
    }

    private BlockingQueue<String> getValidatedQueue(IClientSession clientSession, IOUtils io) {
        String queueName = clientSession.getLastDeclaredQueue();
        if (queueName == null) {
            io.printError("no queue declared");
            return null;
        }

        BlockingQueue<String> queue = queueMap.get(queueName);
        if (queue == null) {
            io.printError("queue does not exist");
            System.err.printf("Server error: Client-declared queue `%s` does not exist%n", queueName);
        }
        return queue;
    }
}
