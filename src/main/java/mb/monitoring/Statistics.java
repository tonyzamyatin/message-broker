package mb.monitoring;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

public class Statistics {
    private final Map<String, ServerStatistic> serverStatistics = new HashMap<>();

    public void recordUsage(Inet4Address ip, int port, String routingKey) {
        String key = ip.getHostAddress() + ":" + port;
        ServerStatistic serverStatistic = serverStatistics.computeIfAbsent(key, k -> new ServerStatistic(ip, port));
        serverStatistic.incrementRoutingKey(routingKey);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (var key : serverStatistics.keySet()) {
            sb.append(serverStatistics.get(key).toString());
        }
        return sb.toString();
    }
}
