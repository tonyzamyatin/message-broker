package mb.utils;

public class LoggingUtil {

    private static final int MAX_LEVEL_PADDING = 7; // Adjust based on length of class and method names (and CLI visuals)
    private static final int MAX_LOCATION_PADDING = 35;

    /**
     * Logs a message with a specified level, optional exception, and includes the location of the log message.
     *
     * @param level   the log level (e.g., ERROR, WARNING, INFO)
     * @param format  the format string
     * @param e       the exception to log (optional, can be null)
     * @param args    the arguments referenced by the format specifiers in the format string
     */
    private static void log(String level, String format, Exception e, Object... args) {
        // Retrieve caller information
        StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
        String location = String.format("%s:%s",
                caller.getFileName(),
                pad(String.valueOf(caller.getLineNumber()), 3, true));

        // Log message with timestamp, level, and padded location
        System.out.printf("[%tF %<tT] %s [%s]: %s%n",
                System.currentTimeMillis(),
                pad(level, MAX_LEVEL_PADDING, false),
                pad(location, MAX_LOCATION_PADDING, true),
                String.format(format, args));

        if (e != null) {
            e.printStackTrace(System.out);
        }
    }

    private static String pad(String str, int maxPadding, boolean padLeft) {
        int paddingLength = Math.max(0, maxPadding - str.length());
        String padding = " ".repeat(paddingLength);
        return padLeft ? padding + str : str + padding;
    }

    /**
     * Logs an error message with a formatted string.
     *
     * @param format the format string
     * @param args   the arguments referenced by the format specifiers in the format string
     */
    public static void logErrorMsg(String format, Object... args) {
        log("ERROR", format, null, args);
    }

    /**
     * Logs an error message with an exception and a formatted string.
     *
     * @param e      the exception to log
     * @param format the format string
     * @param args   the arguments referenced by the format specifiers in the format string
     */
    public static void logErrorMsg(Exception e, String format, Object... args) {
        log("ERROR", format, e, args);
    }

    /**
     * Logs a warning message with a formatted string.
     *
     * @param format the format string
     * @param args   the arguments referenced by the format specifiers in the format string
     */
    public static void logWarningMsg(String format, Object... args) {
        log("WARNING", format, null, args);
    }

    /**
     * Logs an informational message with a formatted string.
     *
     * @param format the format string
     * @param args   the arguments referenced by the format specifiers in the format string
     */
    public static void logInfoMsg(String format, Object... args) {
        log("INFO", format, null, args);
    }
}
