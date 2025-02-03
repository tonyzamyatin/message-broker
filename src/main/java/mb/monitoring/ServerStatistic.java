package mb.monitoring;

import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

public class ServerStatistic {
    private final Map<String, Long> routingKeyCounts = new HashMap<>();
    private final Inet4Address ip;
    private final int port;


    public ServerStatistic(Inet4Address ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void incrementRoutingKey(String routingKey) {
        routingKeyCounts.compute(routingKey, (k, v) -> (v == null) ? 1 : v + 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Server %s:%d\n", ip.getHostAddress(), port));
        for (String key : routingKeyCounts.keySet()) {
            sb.append(String.format("\t%s %d\n", key, routingKeyCounts.get(key)));
        }
        return sb.toString();
    }
}
