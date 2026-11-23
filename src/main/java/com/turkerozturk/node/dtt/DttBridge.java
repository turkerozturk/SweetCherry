package com.turkerozturk.node.dtt;

import com.turkerozturk.markdown.MarkdownRenderer;
import com.turkerozturk.markdown.NodeMarkup;
import com.turkerozturk.multipledatabases.TenantContext;
import com.turkerozturk.multipledatabases.TenantService;
import com.turkerozturk.node.Node;
import com.turkerozturk.node.tags.NodeTags;
import com.turkerozturk.richtext.experimental.RichTextDocument;
import com.turkerozturk.richtext.experimental.RichTextXmlReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class DttBridge {
    private static final String BASE_URL = "custom.dtt.baseUrl";
    private static final String TOKEN_ENV = "custom.dtt.tokenEnv";
    private static final String API_PATH = "/api/bridge/v1/topics/";
    private final TenantService tenants;
    private final Environment environment;
    private final RestClient client;
    @Value("${myapp.dtt.topic-import.enabled:true}")
    private boolean topicImportEnabled = true;

    public DttBridge(TenantService tenants, Environment environment) {
        this.tenants = tenants;
        this.environment = environment;
        var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        var factory = new JdkClientHttpRequestFactory(http);
        factory.setReadTimeout(Duration.ofSeconds(3));
        client = RestClient.builder().requestFactory(factory).build();
    }

    public record Snapshot(String topicId, String topicName, LocalDate date, TodayEntry entry, String revision,
                           TopicDetails details) {}
    public record TopicDetails(String description, String category, String group, String intervalRule,
                               LocalDate latestDoneDate, LocalDate predictionDate, boolean pinned) {}
    public record TodayEntry(Integer status, String note) {}
    public record Update(LocalDate date, String revision, Integer status, String note) {}
    record Target(URI uri, String token, String topicId, String binding) {}

    public boolean populate(Node node, Model model) {
        if (!marked(node)) return false;
        var settings = currentSettings();
        if (settings == null || (!settings.containsKey(BASE_URL) && !settings.containsKey(TOKEN_ENV))) return false;
        try {
            Target target = target(node, settings);
            Snapshot snapshot = exchange(target, client.get().uri(target.uri()), true);
            populatePanel(model, target, snapshot);
        } catch (ResponseStatusException error) {
            model.addAttribute("dttError", error.getReason());
        }
        return true;
    }

    void read(Node node, LocalDate date, Model model) {
        Target target = target(node, currentSettings());
        URI uri = dateUri(target, date);
        Snapshot snapshot = exchange(target, client.get().uri(uri), true);
        populatePanel(model, target, requireDate(snapshot, date));
    }

    void update(Node node, String binding, Update update, boolean delete, boolean dated, Model model) {
        Target target = target(node, currentSettings());
        if (!target.binding().equals(binding)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "dtt.bridge.changed");
        }
        URI entryUri = dated ? dateUri(target, update.date()) : target.uri();
        RestClient.RequestHeadersSpec<?> request;
        if (delete) {
            URI uri = UriComponentsBuilder.fromUri(entryUri)
                    .queryParam("date", update.date()).queryParam("revision", update.revision()).build().encode().toUri();
            request = client.delete().uri(uri);
        } else {
            request = client.put().uri(entryUri).contentType(MediaType.APPLICATION_JSON).body(update);
        }
        populatePanel(model, target, requireDate(exchange(target, request, false), update.date()));
    }

    private static URI dateUri(Target target, LocalDate date) {
        if (date == null || date.getYear() < 1 || date.getYear() > 9999) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return target.uri().resolve("entries/" + date);
    }

    private static Snapshot requireDate(Snapshot snapshot, LocalDate date) {
        if (!date.equals(snapshot.date())) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
        }
        return snapshot;
    }

    private static void populatePanel(Model model, Target target, Snapshot snapshot) {
        model.addAttribute("dttSnapshot", snapshot);
        model.addAttribute("dttBinding", target.binding());
        String description = snapshot.details() == null ? null : snapshot.details().description();
        if (description != null && !description.isBlank() && description.length() <= MarkdownRenderer.MAX_CHARACTERS) {
            model.addAttribute("dttDescriptionHtml", NodeMarkup.render("markdown", description));
        }
    }

    private Snapshot exchange(Target target, RestClient.RequestHeadersSpec<?> request, boolean reading) {
        try {
            Snapshot result = request.headers(headers -> headers.setBearerAuth(target.token()))
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(Snapshot.class);
            if (result == null || !target.topicId().equals(result.topicId()) || result.date() == null
                    || result.revision() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
            }
            return result;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 409) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "dtt.bridge.changed");
            }
            if (reading && error.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "dtt.bridge.missing");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
        }
    }

    public boolean topicImportConfigured() {
        if (!topicImportEnabled || !TenantContext.hasCurrentTenant()) return false;
        var settings = currentSettings();
        return settings != null && !settings.getOrDefault(BASE_URL, "").isBlank()
                && !settings.getOrDefault(TOKEN_ENV, "").isBlank();
    }

    public DttCatalog catalog() {
        if (!topicImportConfigured()) throw configurationError();
        var settings = currentSettings();
        var endpoint = endpoint(settings, "/api/bridge/v1/topics");
        try {
            return client.get().uri(endpoint).headers(headers -> headers.setBearerAuth(token(settings)))
                    .accept(MediaType.APPLICATION_JSON).retrieve().body(DttCatalog.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dttImport.apiMissing");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "dtt.bridge.unavailable");
        }
    }

    private Map<String, String> currentSettings() {
        return tenants.getCustomProperties(TenantContext.getCurrentTenant());
    }

    private Target target(Node node, Map<String, String> settings) {
        if (!marked(node)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "dtt.bridge.invalidId");
        String id = topicId(node);
        URI uri = endpoint(settings, API_PATH + id + "/today");
        return new Target(uri, token(settings), id, binding(uri));
    }

    private String token(Map<String, String> settings) {
        if (settings == null) throw configurationError();
        String tokenName = settings.getOrDefault(TOKEN_ENV, "").strip();
        String token = tokenName.matches("[A-Za-z_][A-Za-z0-9_]*") ? environment.getProperty(tokenName) : null;
        if (token == null || !token.matches("[A-Za-z0-9_-]{32,256}")) throw configurationError();
        return token;
    }

    private static URI endpoint(Map<String, String> settings, String path) {
        if (settings == null) throw configurationError();
        URI base = peerUri(settings.getOrDefault(BASE_URL, "").strip());
        return URI.create(base.toASCIIString().replaceFirst("/+$", "") + path);
    }

    static boolean marked(Node node) {
        return node != null && hasTopicTag(node.getTags());
    }

    public static boolean hasTopicTag(String tags) {
        return NodeTags.split(tags).stream().anyMatch("dttid"::equalsIgnoreCase);
    }

    static String topicId(Node node) {
        String text = node.getTxt();
        return topicId(text, node.getSyntax()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "dtt.bridge.invalidId"));
    }

    public static Optional<String> topicId(String text, String syntax) {
        if (text != null && "custom-colors".equals(syntax)) {
            try {
                text = new RichTextXmlReader().read(text).runs().stream()
                        .map(RichTextDocument.TextRun::text).collect(Collectors.joining());
            } catch (IllegalArgumentException invalid) {
                text = null;
            }
        }
        if (text != null) {
            String id = text.strip();
            if (id.matches("[0-9]{1,19}")) {
                try {
                    long numeric = Long.parseLong(id);
                    if (numeric > 0) return Optional.of(Long.toString(numeric));
                } catch (NumberFormatException ignored) {
                    // Above the supported topic ID range.
                }
            }
        }
        return Optional.empty();
    }

    static URI peerUri(String value) {
        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            if (host != null && uri.getRawUserInfo() == null && uri.getRawQuery() == null
                    && uri.getRawFragment() == null && uri.getPort() <= 65535
                    && ("https".equalsIgnoreCase(uri.getScheme())
                    || ("http".equalsIgnoreCase(uri.getScheme()) && Set.of("localhost", "127.0.0.1", "[::1]")
                            .contains(host.toLowerCase(Locale.ROOT))))) {
                return uri.normalize();
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid administrator configuration.
        }
        throw configurationError();
    }

    private static ResponseStatusException configurationError() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "dtt.bridge.config");
    }

    private static String binding(URI uri) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(uri.toASCIIString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
