package mb.utils;

public class ValidationUtils {
    public static boolean invalidArgNum(String[] args, int argNum) {
        return args.length != argNum;
    }

    /**
     * Validates whether the client received the expected response within the given timeout.
     *
     * @param connection           the connection to validate.
     * @param expectedResponse the expected response message.
     * @param timeoutMs        the timeout duration in milliseconds.
     * @return true if the actual response is equal to the expected response, false otherwise.
     */
    public static boolean validateResponseEquals(Client connection, String expectedResponse, int timeoutMs) {
        return expectedResponse.equals(connection.waitForResponse(timeoutMs));
    }

    public static boolean isInt(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
