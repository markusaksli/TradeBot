package data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class FXMacroDataClient {

    private final String apiKey;
    private final String baseUrl;

    public FXMacroDataClient(String apiKey) {
        this(apiKey, "https://api.fxmacrodata.com/v1");
    }

    public FXMacroDataClient(String apiKey, String baseUrl) {
        String key = apiKey == null ? "" : apiKey.trim();
        for (char c : key.toCharArray()) {
            if (Character.isISOControl(c)) throw new IllegalArgumentException("FXMacroData API key contains invalid characters");
        }
        this.apiKey = key;
        this.baseUrl = normalizeBaseUrl(baseUrl);
    }

    public String dataCatalogue(String currency) throws IOException { return get("/data_catalogue/" + norm(currency)); }
    public String announcements(String currency, String indicator) throws IOException { return get("/announcements/" + norm(currency) + "/" + pathSegment(indicator)); }
    public String calendar(String currency) throws IOException { return get("/calendar/" + norm(currency)); }
    public String predictions(String currency, String indicator) throws IOException { return get("/predictions/" + norm(currency) + "/" + pathSegment(indicator)); }
    public String forex(String base, String quote) throws IOException { return get("/forex/" + norm(base) + "/" + norm(quote)); }
    public String cot(String currency) throws IOException { return get("/cot/" + norm(currency)); }
    public String commoditiesLatest() throws IOException { return get("/commodities/latest"); }
    public String commodity(String indicator) throws IOException { return get("/commodities/" + pathSegment(indicator)); }
    public String curves(String currency) throws IOException { return get("/curves/" + norm(currency)); }
    public String curveProxies(String currency) throws IOException { return get("/curve_proxies/" + norm(currency)); }
    public String forwardCurves(String currency) throws IOException { return get("/forward_curves/" + norm(currency)); }
    public String marketSessions() throws IOException { return get("/market_sessions"); }
    public String riskSentiment() throws IOException { return get("/risk_sentiment"); }
    public String news(String currency) throws IOException { return get("/news/" + norm(currency)); }
    public String pressReleases(String currency) throws IOException { return get("/press-releases/" + norm(currency)); }

    // List endpoints return 20 rows by default and at most 100 per request, newest first.
    // Pass limit/offset and follow pagination.next_offset while pagination.has_more is true.
    public String announcements(String currency, String indicator, int limit, int offset) throws IOException { return get("/announcements/" + norm(currency) + "/" + pathSegment(indicator) + page(limit, offset)); }
    public String predictions(String currency, String indicator, int limit, int offset) throws IOException { return get("/predictions/" + norm(currency) + "/" + pathSegment(indicator) + page(limit, offset)); }
    public String forex(String base, String quote, int limit, int offset) throws IOException { return get("/forex/" + norm(base) + "/" + norm(quote) + page(limit, offset)); }
    public String cot(String currency, int limit, int offset) throws IOException { return get("/cot/" + norm(currency) + page(limit, offset)); }
    public String commodity(String indicator, int limit, int offset) throws IOException { return get("/commodities/" + pathSegment(indicator) + page(limit, offset)); }

    private String get(String path) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(buildUrl(path)).toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(20000);
        // Do not follow redirects, so the API key is never re-sent to another host.
        connection.setInstanceFollowRedirects(false);
        if (!apiKey.isEmpty()) connection.setRequestProperty("X-API-Key", apiKey);
        int status = connection.getResponseCode();
        if (status < 200 || status >= 300) throw new IOException("FXMacroData returned HTTP " + status);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) body.append(line);
            return body.toString();
        }
    }

    String buildUrl(String path) {
        return baseUrl + path;
    }

    static String page(int limit, int offset) {
        return "?limit=" + Math.max(1, Math.min(limit, 100)) + "&offset=" + Math.max(0, offset);
    }

    private static String norm(String value) throws IOException {
        return pathSegment(value.trim().toLowerCase(Locale.ROOT));
    }

    private static String pathSegment(String value) throws IOException {
        return URLEncoder.encode(value.trim(), StandardCharsets.UTF_8.name()).replace("+", "%20");
    }

    private static String normalizeBaseUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            if (!("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                    || uri.getHost() == null || uri.getPort() > 65535
                    || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null) {
                throw new IllegalArgumentException();
            }
            String base = uri.toASCIIString();
            return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw new IllegalArgumentException(
                    "FXMacroData base URL must be HTTP(S) without credentials, query or fragment");
        }
    }
}
