package mb.monitoring;

import mb.ComponentFactory;
import mb.config.MonitoringServerConfig;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.SocketException;

import static mb.utils.LoggingUtil.logErrorMsg;
import static mb.utils.ValidationUtils.invalidArgNum;
import static mb.utils.ValidationUtils.isInt;


public class MonitoringServer implements IMonitoringServer {
    private final DatagramSocket socket;
    private int receivedMessages = 0;
    private final Statistics statistics = new Statistics();
    private boolean running;

    public MonitoringServer(MonitoringServerConfig config) {
        try {
            socket = new DatagramSocket(config.monitoringPort());
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        ComponentFactory.createMonitoringServer(args[0]).run();
    }

    @Override
    public void run() {
        running = true;
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        while (running) {
            try {
                socket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());
                String[] args = message.split("[ :]");
                if (invalidArgNum(args, 3) || !isInt(args[1]) || args[1].length() > 5) {
                    continue;   // invalid message
                }
                Inet4Address ip = (Inet4Address) Inet4Address.getByName(args[0]);
                int port = Integer.parseInt(args[1]);
                String routingKey = args[2];

                statistics.recordUsage(ip, port, routingKey);
                receivedMessages++;
            } catch (IOException | ClassCastException e) {
                if (!running) return;
                logErrorMsg(e, "Failed to receive UDP packet: %s", e.getMessage());
            }
        }
    }

    @Override
    public void shutdown() {
        running = false;
        socket.close();
    }

    @Override
    public int receivedMessages() {
        return receivedMessages;
    }

    @Override
    public String getStatistics() {
        return statistics.toString();
    }
}
