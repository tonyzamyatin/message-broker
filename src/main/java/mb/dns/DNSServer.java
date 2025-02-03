package mb.dns;

import mb.utils.ValidationUtils;
import mb.ComponentFactory;
import mb.utils.IOUtils;
import mb.lifecycle.ServerLifecycleManager;
import mb.config.DNSServerConfig;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DNSServer implements IDNSServer {
    private final ServerLifecycleManager serverLifecycleManager;

    private final ConcurrentMap<String, String> domainNameMappings = new ConcurrentHashMap<>();

    public DNSServer(DNSServerConfig config) {
        serverLifecycleManager = new ServerLifecycleManager(config.port(), this::handleSDPConnection);
    }

    public static void main(String[] args) {
        ComponentFactory.createDNSServer(args[0]).run();
    }

    @Override
    public void run() {
        serverLifecycleManager.run();
    }

    @Override
    public void shutdown() {
        serverLifecycleManager.shutdown();
        domainNameMappings.clear();
    }

    private void handleSDPConnection(Socket SDPConnection) throws IOException {
        try (
                Socket connection = SDPConnection;
                IOUtils io = new IOUtils(connection.getInputStream(), connection.getOutputStream())
        ) {
            io.sendMessage("ok SDP");

            String inputLine;
            while (!connection.isClosed() && (inputLine = io.readMessage()) != null) {
                if (inputLine.isEmpty()) {
                    continue;
                }

                String[] inputArgs = inputLine.split(" ");
                String cmd = inputArgs[0];
                switch (cmd) {
                    case "register":
                        handleRegister(inputArgs, io);
                        break;
                    case "unregister":
                        handleUnregister(inputArgs, io);
                        break;
                    case "resolve":
                        handleResolve(inputArgs, io);
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

    private void handleRegister(String[] args, IOUtils io) {
        if (ValidationUtils.invalidArgNum(args, 3)) {
            io.printUsage("register <name> <ip:port>");
            return;
        }
        String name = args[1];
        String address = args[2];
        domainNameMappings.put(name, address);
        io.sendMessage("ok");
    }

    private void handleUnregister(String[] args, IOUtils io) {
        if(ValidationUtils.invalidArgNum(args, 2)) {
            io.printUsage("unregister <name>");
            return;
        }
        String name = args[1];
        domainNameMappings.remove(name);
        io.sendMessage("ok");
    }

    private void handleResolve(String[] args, IOUtils io) {
        if(ValidationUtils.invalidArgNum(args, 2)) {
            io.printUsage("resolve <name>");
            return;
        }
        String name = args[1];
        String address = domainNameMappings.get(name);
        if (address != null) {
            io.sendMessage(address);
        } else {
            io.printError("domain not found");
        }
    }
}
