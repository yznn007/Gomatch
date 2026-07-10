package com.gomatch.server.security;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Small in-memory fixed-window limiter for protocol abuse protection. */
public final class FixedWindowRateLimiter<K> {
    private static final long CLEANUP_INTERVAL = 256;

    private final int limit;
    private final long windowMillis;
    private final LongSupplier clock;
    private final ConcurrentHashMap<K, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong acquisitions = new AtomicLong();

    public FixedWindowRateLimiter(int limit, long windowMillis) {
        this(limit, windowMillis, System::currentTimeMillis);
    }

    FixedWindowRateLimiter(int limit, long windowMillis, LongSupplier clock) {
        if (limit < 1 || windowMillis < 1) {
            throw new IllegalArgumentException("limit and windowMillis must be positive");
        }
        this.limit = limit;
        this.windowMillis = windowMillis;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public boolean tryAcquire(K key) {
        Objects.requireNonNull(key, "key");
        long now = clock.getAsLong();
        AtomicBoolean allowed = new AtomicBoolean();
        windows.compute(key, (ignored, existing) -> {
            if (existing == null || now < existing.startedAt || now - existing.startedAt >= windowMillis) {
                allowed.set(true);
                return new Window(now, 1);
            }
            if (existing.count >= limit) {
                return existing;
            }
            allowed.set(true);
            return new Window(existing.startedAt, existing.count + 1);
        });
        if (acquisitions.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            windows.entrySet().removeIf(entry -> now - entry.getValue().startedAt >= windowMillis);
        }
        return allowed.get();
    }

    private record Window(long startedAt, int count) {}
}
