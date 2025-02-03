package mb.config;

import java.util.Objects;

public record DNSConfig(
        String host,
        int port,
        String dnsHost,
        int dnsPort,
        String domain
) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNSConfig dnsConfig)) return false;

        return port == dnsConfig.port && dnsPort == dnsConfig.dnsPort && Objects.equals(host, dnsConfig.host) && Objects.equals(domain, dnsConfig.domain) && Objects.equals(dnsHost, dnsConfig.dnsHost);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(host);
        result = 31 * result + port;
        result = 31 * result + Objects.hashCode(dnsHost);
        result = 31 * result + dnsPort;
        result = 31 * result + Objects.hashCode(domain);
        return result;
    }
}
