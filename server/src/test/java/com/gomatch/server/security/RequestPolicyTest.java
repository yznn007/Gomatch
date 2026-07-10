package com.gomatch.server.security;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.junit.jupiter.api.Assertions.*;

class RequestPolicyTest {

    @Test
    void clampsPaginationBeforeDispatch() throws Exception {
        ProtocolRequestGuard guard = ProtocolRequestGuard.forTesting(100, 100, 100);
        String normalized = guard.validateAndNormalize(
                "{\"cmd\":\"QUERY_RECORD\",\"data\":{\"page\":-4,\"size\":9999}}",
                InetAddress.getLoopbackAddress());

        JSONObject data = new JSONObject(normalized).getJSONObject("data");
        assertEquals(0, data.getInt("page"));
        assertEquals(100, data.getInt("size"));
    }

    @Test
    void rejectsOversizeChatAndAvatarBeforeDispatch() throws Exception {
        ProtocolRequestGuard guard = ProtocolRequestGuard.forTesting(100, 100, 100);
        InetAddress address = InetAddress.getLoopbackAddress();

        String chat = new JSONObject().put("cmd", "CHAT").put("data",
                new JSONObject().put("message", "x".repeat(501))).toString();
        assertThrows(ProtocolRequestGuard.ProtocolRejectedException.class,
                () -> guard.validateAndNormalize(chat, address));

        String avatar = new JSONObject().put("cmd", "UPLOAD_AVATAR").put("data",
                new JSONObject().put("data", "x".repeat(SecurityLimits.MAX_AVATAR_BASE64_CHARS + 1))).toString();
        assertThrows(ProtocolRequestGuard.ProtocolRejectedException.class,
                () -> guard.validateAndNormalize(avatar, address));
    }

    @Test
    void rateLimitsLoginByAddressAndUsername() throws Exception {
        ProtocolRequestGuard guard = ProtocolRequestGuard.forTesting(100, 2, 100);
        InetAddress address = InetAddress.getLoopbackAddress();
        String login = "{\"cmd\":\"LOGIN\",\"data\":{\"username\":\"Alice\",\"password\":\"secret\"}}";

        assertDoesNotThrow(() -> guard.validateAndNormalize(login, address));
        assertDoesNotThrow(() -> guard.validateAndNormalize(login, address));
        assertThrows(ProtocolRequestGuard.ProtocolRejectedException.class,
                () -> guard.validateAndNormalize(login, address));
    }
}
