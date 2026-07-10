package com.gomatch.server.security;

import java.net.InetAddress;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Atomically enforces both global and per-address connection limits. */
public final class ConnectionGuard {
    private final int totalLimit;
    private final int perAddressLimit;
    private final AtomicInteger total = new AtomicInteger();
    private final ConcurrentHashMap<InetAddress, AtomicInteger> byAddress = new ConcurrentHashMap<>();

    public ConnectionGuard(int totalLimit, int perAddressLimit) {
        if (totalLimit < 1 || perAddressLimit < 1) {
            throw new IllegalArgumentException("connection limits must be positive");
        }
        this.totalLimit = totalLimit;
        this.perAddressLimit = perAddressLimit;
    }

    public synchronized boolean tryAcquire(InetAddress address) {
        Objects.requireNonNull(address, "address");
        int addressCount = byAddress.getOrDefault(address, new AtomicInteger()).get();
        if (total.get() >= totalLimit || addressCount >= perAddressLimit) {
            return false;
        }
        total.incrementAndGet();
        byAddress.computeIfAbsent(address, ignored -> new AtomicInteger()).incrementAndGet();
        return true;
    }

    public synchronized void release(InetAddress address) {
        if (address == null) {
            return;
        }
        AtomicInteger addressCount = byAddress.get(address);
        if (addressCount == null || addressCount.get() <= 0) {
            return;
        }
        if (addressCount.decrementAndGet() == 0) {
            byAddress.remove(address, addressCount);
        }
        total.updateAndGet(value -> Math.max(0, value - 1));
    }

    public int getTotal() {
        return total.get();
    }

    public int getForAddress(InetAddress address) {
        AtomicInteger value = byAddress.get(address);
        return value == null ? 0 : value.get();
    }
}
