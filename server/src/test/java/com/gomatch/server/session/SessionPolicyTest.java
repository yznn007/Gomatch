package com.gomatch.server.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SessionPolicyTest {

    @Test
    void rejectsExpiredInactiveAndConcurrentSessions() {
        Instant now = Instant.parse("2026-07-10T12:00:00Z");

        assertEquals(SessionAuthorizationPolicy.Decision.EXPIRED,
                SessionAuthorizationPolicy.decide(now.minusSeconds(301), now, false, true));
        assertEquals(SessionAuthorizationPolicy.Decision.ACCOUNT_DISABLED,
                SessionAuthorizationPolicy.decide(now, now, false, false));
        assertEquals(SessionAuthorizationPolicy.Decision.ALREADY_CONNECTED,
                SessionAuthorizationPolicy.decide(now, now, true, true));
        assertEquals(SessionAuthorizationPolicy.Decision.ALLOW,
                SessionAuthorizationPolicy.decide(now, now, false, true));
    }
}
