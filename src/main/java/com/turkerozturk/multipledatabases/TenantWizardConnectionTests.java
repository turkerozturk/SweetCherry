package com.turkerozturk.multipledatabases;

import com.turkerozturk.node.dtt.DttBridge;
import com.turkerozturk.security.LocalSqliteConnections;
import jakarta.annotation.PreDestroy;
import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteOpenMode;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class TenantWizardConnectionTests {
    private static final long DEADLINE_MS = 6_000;
    private static final Map<String, Set<String>> TLS_OPTIONS = Map.of(
            "postgresql:sslmode", Set.of("disable", "allow", "prefer", "require", "verify-ca", "verify-full"),
            "postgresql:ssl", Set.of("true", "false"),
            "mariadb:sslMode", Set.of("disable", "trust", "verify-ca", "verify-full"),
            "mysql:sslMode", Set.of("DISABLED", "PREFERRED", "REQUIRED", "VERIFY_CA", "VERIFY_IDENTITY"),
            "mysql:useSSL", Set.of("true", "false"),
            "mysql:requireSSL", Set.of("true", "false"),
            "mysql:verifyServerCertificate", Set.of("true", "false"),
            "mysql:allowPublicKeyRetrieval", Set.of("true", "false"));

    private final TenantWizardService wizard;
    private final DttBridge bridge;
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new SynchronousQueue<>(), task -> {
                var worker = new Thread(task, "tenant-connection-test");
                worker.setDaemon(true);
                return worker;
            });

    public TenantWizardConnectionTests(TenantWizardService wizard, DttBridge bridge) {
        this.wizard = wizard;
        this.bridge = bridge;
    }

    @PostMapping("/tenants/wizard/test-dtt")
    public DeferredResult<ResponseEntity<TestResult>> dtt(@RequestParam(defaultValue = "") String dttBaseUrl,
            @RequestParam(defaultValue = "") String dttTokenEnv) {
        return run(() -> bridge.testWizardConnection(dttBaseUrl, dttTokenEnv) ? "success" : "failed");
    }

    @PostMapping("/tenants/wizard/test-database")
    public DeferredResult<ResponseEntity<TestResult>> database(@RequestParam String type,
            @RequestParam(defaultValue = "") String path, @RequestParam(defaultValue = "") String url,
            @RequestParam(defaultValue = "") String username, @RequestParam(defaultValue = "") String password,
            @RequestParam(defaultValue = "") String tenant, @RequestParam(defaultValue = "") String revision,
            @RequestParam(defaultValue = "false") boolean createEmpty,
            @RequestParam(defaultValue = "false") boolean clearPassword) {
        return run(() -> {
            if ("sqlite".equals(type)) {
                if (createEmpty) return "saveFirst";
                var config = new SQLiteConfig();
                config.setReadOnly(true);
                config.resetOpenMode(SQLiteOpenMode.CREATE);
                config.setBusyTimeout(1_000);
                try (var connection = LocalSqliteConnections.open(TenantWizardService.ctb(path), config)) {
                    checkTables(connection);
                }
            } else {
                if (username.length() > 1024 || password.length() > 1024) return "invalid";
                var target = remoteTarget(type, url);
                String secret = clearPassword ? "" : password;
                if (!clearPassword && secret.isEmpty()) {
                    secret = wizard.savedTestPassword(tenant, revision, TenantWizardService.DRIVERS.get(type),
                            url.strip(), username);
                }
                target.properties().setProperty("user", username);
                target.properties().setProperty("password", secret);
                // A standalone connection, independent of the registered tenant pools.
                try (var connection = DriverManager.getConnection(target.url(), target.properties())) {
                    connection.setReadOnly(true);
                    checkTables(connection);
                }
            }
            return "success";
        });
    }

    record RemoteTarget(String url, Properties properties) {}

    static RemoteTarget remoteTarget(String type, String url) {
        if (!Set.of("postgresql", "mariadb", "mysql").contains(type) || url.length() > 4096
                || !url.strip().startsWith("jdbc:" + type + "://")) {
            throw new IllegalArgumentException("wizard.invalidUrl");
        }
        URI uri = URI.create(url.strip().substring(5));
        String path = uri.getRawPath();
        if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawFragment() != null
                || uri.getPort() == 0 || uri.getPort() > 65535 || path == null || !path.matches("/[A-Za-z0-9_.$-]+")) {
            throw new IllegalArgumentException("wizard.invalidUrl");
        }
        var properties = new Properties();
        if (uri.getRawQuery() != null) {
            for (String option : uri.getRawQuery().split("&", -1)) {
                String[] parts = option.split("=", 2);
                if (parts.length != 2 || !TLS_OPTIONS.getOrDefault(type + ":" + parts[0], Set.of()).contains(parts[1])
                        || properties.putIfAbsent(parts[0], parts[1]) != null) {
                    throw new IllegalArgumentException("wizard.test.options");
                }
            }
        }
        String timeout = "postgresql".equals(type) ? "2" : "2000";
        properties.setProperty("connectTimeout", timeout);
        properties.setProperty("socketTimeout", timeout);
        return new RemoteTarget("jdbc:" + type + "://" + uri.getRawAuthority() + path, properties);
    }

    private static void checkTables(Connection connection) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.setQueryTimeout(2);
            try (var ignored = statement.executeQuery("SELECT node_id FROM node WHERE 1 = 0")) { }
            try (var ignored = statement.executeQuery("SELECT node_id, father_id FROM children WHERE 1 = 0")) { }
        }
    }

    public record TestResult(String result) {}

    private DeferredResult<ResponseEntity<TestResult>> run(Callable<String> check) {
        var result = new DeferredResult<ResponseEntity<TestResult>>(DEADLINE_MS);
        try {
            var task = workers.submit(() -> {
                String outcome;
                try {
                    outcome = check.call();
                } catch (IllegalArgumentException error) {
                    outcome = switch (String.valueOf(error.getMessage())) {
                        case "wizard.dttTarget" -> "target";
                        case "wizard.test.dttRequired" -> "dttRequired";
                        case "wizard.test.dttTokenMissing" -> "tokenMissing";
                        case "wizard.test.reenterPassword" -> "password";
                        case "wizard.test.options" -> "options";
                        case "wizard.stale" -> "stale";
                        default -> "invalid";
                    };
                } catch (Exception error) {
                    outcome = "failed";
                }
                result.setResult(response(outcome));
            });
            result.onTimeout(() -> {
                result.setResult(response("timeout"));
                task.cancel(true);
            });
            result.onError(error -> task.cancel(true));
        } catch (RejectedExecutionException error) {
            result.setResult(response("busy"));
        }
        return result;
    }

    private static ResponseEntity<TestResult> response(String code) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TestResult(code));
    }

    @PreDestroy
    void stop() {
        workers.shutdownNow();
    }
}
