package com.turkerozturk.login;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Limits failed logins by account and a separately verified client address. */
@Component
public class LoginAttemptLimiter {
    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptLimiter.class);
    private static final int MAX_FAILURES = 5;
    private static final int MAX_ENTRIES = 4096;
    private static final Duration WINDOW = Duration.ofMinutes(15);
    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();
    private final AtomicInteger operations = new AtomicInteger();
    private final Clock clock;

    public LoginAttemptLimiter() {
        this(Clock.systemUTC());
    }

    LoginAttemptLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Checks whether failed attempts for this account and peer reached the limit. */
    public boolean blocked(String username, String peer, String userName, String adminName) {
        Attempt attempt = attempts.get(key(username, peer, userName, adminName));
        return attempt != null && clock.instant().isBefore(attempt.expiresAt())
                && attempt.count() >= MAX_FAILURES;
    }

    /** Records an unsuccessful authentication, resetting an expired attempt window. */
    public void failed(String username, String peer, String userName, String adminName) {
        Instant now = clock.instant();
        Attempt current = attempts.compute(key(username, peer, userName, adminName), (ignored, previous) ->
                previous == null || !now.isBefore(previous.expiresAt())
                        ? new Attempt(1, now.plus(WINDOW))
                        : new Attempt(previous.count() + 1, previous.expiresAt()));
        String account = accountLabel(username, userName, adminName);
        logger.warn("Login failed: account={}, clientIp={}, attempts={}/{}",
                account, peer, current.count(), MAX_FAILURES);
        if (current.count() == MAX_FAILURES) {
            logger.warn("Login temporarily blocked: account={}, clientIp={}, until={}",
                    account, peer, current.expiresAt());
        }
        if (operations.incrementAndGet() % 128 == 0) {
            attempts.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().expiresAt()));
            if (attempts.size() > MAX_ENTRIES) attempts.clear();
        }
    }

    /** Clears the successful account's failed attempts for this peer. */
    public void succeeded(String username, String peer, String userName, String adminName) {
        attempts.remove(key(username, peer, userName, adminName));
        logger.info("Login succeeded: account={}, clientIp={}",
                accountLabel(username, userName, adminName), peer);
    }

    /** Shows configured account names while avoiding attacker controlled text in security logs. */
    private String accountLabel(String username, String userName, String adminName) {
        if (username != null && username.equalsIgnoreCase(userName)) return userName;
        if (username != null && username.equalsIgnoreCase(adminName)) return adminName;
        return "<unknown>";
    }

    /** Groups nonexistent usernames to prevent unbounded keys from username spraying. */
    private String key(String username, String peer, String userName, String adminName) {
        String account = username == null ? "" : username.toLowerCase(Locale.ROOT);
        if (!account.equals(userName.toLowerCase(Locale.ROOT))
                && !account.equals(adminName.toLowerCase(Locale.ROOT))) account = "<unknown>";
        return (peer == null ? "" : peer) + '\u0000' + account;
    }

    private record Attempt(int count, Instant expiresAt) { }
}
