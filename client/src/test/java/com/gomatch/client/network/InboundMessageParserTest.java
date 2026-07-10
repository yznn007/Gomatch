package com.gomatch.client.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InboundMessageParserTest {
    @Test
    void malformedFrameDoesNotPoisonFollowingFrame() {
        assertTrue(InboundMessageParser.parse("not-json").isEmpty());
        assertEquals("HEARTBEAT_OK",
                InboundMessageParser.parse("{\"cmd\":\"HEARTBEAT_OK\"}").orElseThrow().getCmd());
    }
}
