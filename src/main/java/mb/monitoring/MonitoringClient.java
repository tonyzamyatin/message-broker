package mb.monitoring;

import mb.config.MonitoringClientConfig;

import java.io.IOException;
import java.net.*;

import static mb.utils.LoggingUtil.logErrorMsg;

public class MonitoringClient {
    private final MonitoringClientConfig config;
    private Inet4Address monitorAddress;
    private DatagramSocket socket;

    public MonitoringClient(MonitoringClientConfig config) {
        this.config = config;
        try {
            monitorAddress = (Inet4Address) Inet4Address.getByName(config.monitorHost());
            socket = new DatagramSocket();
        } catch (SocketException | UnknownHostException e) {
            logErrorMsg(e, "Failed to initialize monitoring client: ", e.getMessage());
        }
    }

    public void sendMessage(String routingKey) {
        if (socket.isClosed()) return;

        String message = String.format("%s:%d %s", config.serverHost(), config.serverPort(), routingKey);
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, monitorAddress, config.monitorPort());
        try {
            socket.send(packet);
        } catch (IOException e) {
            logErrorMsg(e, "Failed to send datagram packed to monitor: ", e.getMessage());
        }
    }

    public void shutdown() {
        socket.close();
    }

}
