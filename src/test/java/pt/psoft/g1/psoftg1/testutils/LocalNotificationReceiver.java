package pt.psoft.g1.psoftg1.testutils;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Local HTTP receiver standing in for the SMS gateway and the readers' webhook targets in collaboration and
 * system tests (TST-NOT-05); {@link LocalSmtpReceiver} stands in for the mail server. It listens on the loopback address only and records each request, so the real channels
 * run end to end and 0 real messages leave the machine (CON-TST-8). Uses the HTTP server shipped with the JDK.
 */
public final class LocalNotificationReceiver implements AutoCloseable {

    public record Received(String path, String body) {}

    private final HttpServer server;
    private final List<Received> received = new CopyOnWriteArrayList<>();

    private LocalNotificationReceiver(HttpServer server) {
        this.server = server;
    }

    public static LocalNotificationReceiver start() {
        try {
            final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            final LocalNotificationReceiver receiver = new LocalNotificationReceiver(server);
            server.createContext("/", exchange -> {
                final String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                // Recorded before answering, so the request is visible as soon as the channel's call returns.
                receiver.received.add(new Received(exchange.getRequestURI().getPath(), body));
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
            });
            server.start();
            return receiver;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String url(String path) {
        return "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort() + path;
    }

    public List<Received> receivedAt(String path) {
        return received.stream().filter(r -> r.path().equals(path)).toList();
    }

    public void clear() {
        received.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
