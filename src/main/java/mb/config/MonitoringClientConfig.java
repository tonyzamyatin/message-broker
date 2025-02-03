package mb.config;

public record MonitoringClientConfig(
        String monitorHost,
        int monitorPort,
        String serverHost,
        int serverPort
) {
}
