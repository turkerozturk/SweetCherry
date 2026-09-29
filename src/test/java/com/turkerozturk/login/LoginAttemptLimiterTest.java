package com.turkerozturk.login;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoginAttemptLimiterTest {
    private final MutableClock clock = new MutableClock();
    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(clock);

    @Test
    void locksOnlyFailingAccountAndPeerAndExpiresAfterFifteenMinutes() {
        for (int i = 0; i < 5; i++) limiter.failed("admin", "proxy", "user", "admin");
        assertTrue(limiter.blocked("admin", "proxy", "user", "admin"));
        assertFalse(limiter.blocked("user", "proxy", "user", "admin"));
        assertFalse(limiter.blocked("admin", "local", "user", "admin"));
        clock.advanceSeconds(15 * 60);
        assertFalse(limiter.blocked("admin", "proxy", "user", "admin"));
    }

    @Test
    void successfulLoginClearsFailuresAndUnknownNamesShareOneBucket() {
        for (int i = 0; i < 4; i++) limiter.failed("admin", "proxy", "user", "admin");
        limiter.succeeded("admin", "proxy", "user", "admin");
        limiter.failed("admin", "proxy", "user", "admin");
        assertFalse(limiter.blocked("admin", "proxy", "user", "admin"));
        for (int i = 0; i < 5; i++) limiter.failed("missing" + i, "proxy", "user", "admin");
        assertTrue(limiter.blocked("any-other-name", "proxy", "user", "admin"));
    }

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");
        void advanceSeconds(long seconds) { now = now.plusSeconds(seconds); }
        @Override public ZoneId getZone() { return ZoneId.of("UTC"); }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
