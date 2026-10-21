package data;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Standalone standard-library regression checks; no live API or test dependencies. */
public final class FXMacroDataClientUrlChecks {
    private static int passed;
    private static int failed;

    private interface Check { void run() throws Exception; }
    private interface Call { String run(String base) throws Exception; }

    private static void check(String name, Check action) {
        try {
            action.run();
            passed++;
            System.out.println("PASS " + name);
        } catch (Exception | AssertionError failure) {
            failed++;
            System.out.println("FAIL " + name + ": " + failure.getClass().getSimpleName());
        }
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) throw new AssertionError("unexpected request or result");
    }

    private static void rejectsBase(String base) {
        try {
            new FXMacroDataClient("fixture-key", base);
        } catch (IllegalArgumentException expected) {
            if (expected.getMessage().contains("private-marker")) throw new AssertionError("base leaked");
            return;
        }
        throw new AssertionError("invalid base accepted");
    }

    private static List<String> capture(Call action) throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            server.setSoTimeout(3000);
            FutureTask<List<String>> received = new FutureTask<>(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(3000);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(
                            socket.getInputStream(), StandardCharsets.US_ASCII));
                    List<String> lines = new ArrayList<>();
                    String line;
                    while ((line = reader.readLine()) != null && !line.isEmpty()) lines.add(line);
                    socket.getOutputStream().write(("HTTP/1.1 200 OK\r\nContent-Length: 2\r\n"
                            + "Content-Type: application/json\r\nConnection: close\r\n\r\n{}")
                            .getBytes(StandardCharsets.US_ASCII));
                    return lines;
                }
            });
            Thread worker = new Thread(received, "loopback-fixture");
            worker.setDaemon(true);
            worker.start();
            equal("{}", action.run("http://127.0.0.1:" + server.getLocalPort() + "/v1"));
            return received.get(4, TimeUnit.SECONDS);
        }
    }

    public static void main(String[] args) throws Exception {
        check("base_query", () -> rejectsBase("https://example.com/v1?private-marker=1"));
        check("base_fragment", () -> rejectsBase("https://example.com/v1#private-marker"));
        check("base_credentials", () -> rejectsBase("https://private-marker@example.com/v1"));
        check("base_scheme", () -> rejectsBase("ftp://example.com/v1"));
        check("base_relative", () -> rejectsBase("/v1"));
        check("path_announcements", () -> equal("GET /v1/announcements/usd/jobs%20%2F%3F%23%25%C3%A9 HTTP/1.1",
                capture(base -> new FXMacroDataClient(null, base).announcements("USD", "jobs /?#%é")).get(0)));
        check("path_predictions_paged", () -> equal("GET /v1/predictions/usd/jobs%3Fx%3D1%23part?limit=100&offset=2 HTTP/1.1",
                capture(base -> new FXMacroDataClient(null, base).predictions("USD", "jobs?x=1#part", 100, 2)).get(0)));
        check("path_commodity", () -> equal("GET /v1/commodities/brent%2Fspot%3Fx%3D1 HTTP/1.1",
                capture(base -> new FXMacroDataClient(null, base).commodity("brent/spot?x=1")).get(0)));
        check("path_announcements_paged", () -> equal("GET /v1/announcements/usd/jobs%23part?limit=20&offset=4 HTTP/1.1",
                capture(base -> new FXMacroDataClient(null, base).announcements("USD", "jobs#part", 20, 4)).get(0)));
        check("path_currencies", () -> equal("GET /v1/forex/eu%2Fr/usd%3Fx%3D1 HTTP/1.1",
                capture(base -> new FXMacroDataClient(null, base).forex("EU/R", "USD?x=1")).get(0)));
        check("control_authenticated_forex", () -> {
            List<String> lines = capture(base -> new FXMacroDataClient("fixture-key", base).forex(" EUR ", "USD"));
            equal("GET /v1/forex/eur/usd HTTP/1.1", lines.get(0));
            if (!lines.contains("X-API-Key: fixture-key")) throw new AssertionError("missing header");
        });
        check("control_keyless_calendar", () -> {
            List<String> lines = capture(base -> new FXMacroDataClient(null, base).calendar("USD"));
            equal("GET /v1/calendar/usd HTTP/1.1", lines.get(0));
            for (String line : lines) {
                if (line.toLowerCase(java.util.Locale.ROOT).startsWith("x-api-key:")) {
                    throw new AssertionError("unexpected header");
                }
            }
        });
        check("control_trailing_slash", () -> {
            FXMacroDataClient client = new FXMacroDataClient(null, "https://example.com/v1/");
            equal("https://example.com/v1/calendar/usd", client.buildUrl("/calendar/usd"));
        });
        check("control_page_clamping", () -> equal("?limit=100&offset=0", FXMacroDataClient.page(101, -1)));
        System.out.println("RESULT passed=" + passed + " failed=" + failed);
        if (failed != 0) throw new AssertionError("URL regression checks failed");
    }
}
