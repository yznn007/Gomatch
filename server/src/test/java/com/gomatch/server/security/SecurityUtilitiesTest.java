package com.gomatch.server.security;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilitiesTest {

    @Test
    void readsLinesWithinLimitAndStripsCarriageReturn() throws Exception {
        var reader = new BoundedLineReader(new StringReader("hello\r\nworld\n"), 5);

        assertEquals("hello", reader.readLine());
        assertEquals("world", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    void rejectsLinePastLimit() {
        var reader = new BoundedLineReader(new StringReader("123456\n"), 5);

        assertThrows(BoundedLineReader.LineTooLongException.class, reader::readLine);
    }

    @Test
    void limiterResetsAfterWindow() {
        AtomicLong now = new AtomicLong(1_000);
        var limiter = new FixedWindowRateLimiter<String>(2, 1_000, now::get);

        assertTrue(limiter.tryAcquire("ip"));
        assertTrue(limiter.tryAcquire("ip"));
        assertFalse(limiter.tryAcquire("ip"));
        now.set(2_001);
        assertTrue(limiter.tryAcquire("ip"));
    }

    @Test
    void connectionGuardAppliesPerIpAndTotalLimits() throws Exception {
        var guard = new ConnectionGuard(2, 1);
        InetAddress one = InetAddress.getByName("127.0.0.1");
        InetAddress two = InetAddress.getByName("127.0.0.2");

        assertTrue(guard.tryAcquire(one));
        assertFalse(guard.tryAcquire(one));
        assertTrue(guard.tryAcquire(two));
        assertFalse(guard.tryAcquire(InetAddress.getByName("127.0.0.3")));
        guard.release(one);
        assertTrue(guard.tryAcquire(one));
    }

    @Test
    void clampsPaginationAndTextLengths() {
        assertEquals(0, SecurityLimits.page(-1));
        assertEquals(100, SecurityLimits.pageSize(Integer.MAX_VALUE));
        assertEquals(1, SecurityLimits.pageSize(0));
        assertTrue(SecurityLimits.validLength("hello", 1, 5));
        assertFalse(SecurityLimits.validLength("hello!", 1, 5));
    }
}
