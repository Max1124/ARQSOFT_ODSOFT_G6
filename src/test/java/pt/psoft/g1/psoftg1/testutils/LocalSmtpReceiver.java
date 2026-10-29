package pt.psoft.g1.psoftg1.testutils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local SMTP server standing in for the mail server of the email channel in collaboration and system tests
 * (TST-NOT-05). It listens on the loopback address only, answers the basic SMTP commands and records each message
 * instead of delivering it, so the real channel runs end to end and 0 real messages leave the machine (CON-TST-8).
 * It offers no extensions (no AUTH, no STARTTLS). Uses only the JDK and the mail library the application ships.
 */
public final class LocalSmtpReceiver implements AutoCloseable {

    public record Received(List<String> recipients, String subject, String text) {}

    private final ServerSocket server;
    private final List<Received> received = new CopyOnWriteArrayList<>();

    private LocalSmtpReceiver(ServerSocket server) {
        this.server = server;
    }

    public static LocalSmtpReceiver start() {
        try {
            final LocalSmtpReceiver receiver = new LocalSmtpReceiver(new ServerSocket(0, 50, InetAddress.getLoopbackAddress()));
            final Thread acceptor = new Thread(receiver::acceptConnections, "local-smtp-receiver");
            acceptor.setDaemon(true);
            acceptor.start();
            return receiver;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String host() {
        return server.getInetAddress().getHostAddress();
    }

    public int port() {
        return server.getLocalPort();
    }

    public List<Received> received() {
        return List.copyOf(received);
    }

    public void clear() {
        received.clear();
    }

    @Override
    public void close() {
        try {
            server.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void acceptConnections() {
        while (!server.isClosed()) {
            try (Socket client = server.accept()) {
                converse(client);
            } catch (IOException e) {
                // closed while waiting, or the client left mid-conversation: wait for the next one
            }
        }
    }

    private void converse(Socket client) throws IOException {
        final BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
        final Writer out = new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8);
        reply(out, "220 localhost");
        final List<String> recipients = new ArrayList<>();
        String line;
        while ((line = in.readLine()) != null) {
            final String command = line.toUpperCase();
            if (command.startsWith("RCPT TO:")) {
                recipients.add(line.substring("RCPT TO:".length()).trim().replaceAll("[<>]", ""));
                reply(out, "250 OK");
            } else if (command.equals("DATA")) {
                reply(out, "354 End data with <CR><LF>.<CR><LF>");
                // Recorded before answering, so the message is visible as soon as the channel's call returns.
                received.add(parse(List.copyOf(recipients), readData(in)));
                recipients.clear();
                reply(out, "250 OK");
            } else if (command.equals("QUIT")) {
                reply(out, "221 Bye");
                return;
            } else {
                reply(out, "250 OK"); // EHLO/HELO, MAIL FROM, RSET, NOOP
            }
        }
    }

    private static String readData(BufferedReader in) throws IOException {
        final StringBuilder data = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null && !line.equals(".")) {
            data.append(line.startsWith("..") ? line.substring(1) : line).append("\r\n");
        }
        return data.toString();
    }

    private static Received parse(List<String> recipients, String data) {
        try {
            final MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)));
            return new Received(recipients, message.getSubject(), String.valueOf(message.getContent()).trim());
        } catch (Exception e) {
            throw new IllegalStateException("Unreadable message received by the local SMTP receiver", e);
        }
    }

    private static void reply(Writer out, String response) throws IOException {
        out.write(response + "\r\n");
        out.flush();
    }
}
