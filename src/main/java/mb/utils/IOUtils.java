package mb.utils;

import java.io.*;

public class IOUtils implements AutoCloseable {
    private final BufferedReader in;
    private final PrintWriter out;

    public IOUtils(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new PrintWriter(new OutputStreamWriter(out), true);
    }

    public void sendMessage(String message) {
        out.println(message);
    }

    public String readMessage() throws IOException {
        return in.readLine();
    }

    public void printError(String errorMsg) {
        out.println("error " + errorMsg);
    }

    public void printUsage(String usage) {
        printError("usage: " + usage);
    }

    @Override
    public void close() throws IOException {
        out.close();
        in.close();
    }

}
