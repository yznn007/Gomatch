package com.gomatch.server.session;

import java.time.Instant;

final class SessionAuthorizationPolicy {
    private static final long TIMEOUT_SECONDS = 300;

    enum Decision { ALLOW, EXPIRED, ACCOUNT_DISABLED, ALREADY_CONNECTED }

    private SessionAuthorizationPolicy() {}

    static Decision decide(Instant lastActive, Instant now, boolean handlerPresent, boolean accountActive) {
        if (lastActive == null || now.isAfter(lastActive.plusSeconds(TIMEOUT_SECONDS))) {
            return Decision.EXPIRED;
        }
        if (!accountActive) {
            return Decision.ACCOUNT_DISABLED;
        }
        if (handlerPresent) {
            return Decision.ALREADY_CONNECTED;
        }
        return Decision.ALLOW;
    }
}
