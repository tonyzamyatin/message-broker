package mb.utils;

public class CommandBuilder {   // Copied from test case util (credits to Ho Li Wang I believe)
    public static final String EXIT = "exit";
    public static final String OK = "ok";
    public static final String ACK = "ack";
    public static final String VOTE = "vote";
    public static final String PING = "ping";
    public static final String PONG = "pong";
    public static final String DECLARE = "declare";
    public static final String ELECT = "elect";
    public final static String SUBSCRIBE = "subscribe";

    public static String exchangeCmd(String type, String name) {
        return String.format("exchange %s %s", type, name);
    }

    public static String queueCmd(String name) {
        return String.format("queue %s", name);
    }

    public static String bindCmd(String bindingKey) {
        return String.format("bind %s", bindingKey);
    }

    public static String publishCmd(String routingKey, String message) {
        return String.format("publish %s %s", routingKey, message);
    }

    public static String resolveCmd(String name) {
        return "resolve %s".formatted(name);
    }

    public static String unregisterCmd(String name) {
        return "unregister %s".formatted(name);
    }

    public static String registerCmd(String name, String ip) {
        return "register %s %s".formatted(name, ip);
    }

    public static String electCmd(int id) {
        return "elect %d".formatted(id);
    }

    public static String declareCmd(int id) {
        return "declare %d".formatted(id);
    }

    public static String ackCmd(int id) {
        return "%s %d".formatted(ACK, id);
    }

    public static String voteCmd(int senderId, int candidateId) {
        return "vote %d %d".formatted(senderId, candidateId);
    }
}