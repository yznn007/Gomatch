package com.gomatch.client.network;

import com.gomatch.common.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/** Isolates one malformed server frame from the long-lived receive loop. */
public final class InboundMessageParser {
    private static final Logger log = LoggerFactory.getLogger(InboundMessageParser.class);

    private InboundMessageParser() {}

    public static Optional<Message> parse(String line) {
        if (line == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Message.fromJson(line));
        } catch (RuntimeException malformed) {
            log.warn("Discarded malformed server message: length={}", line.length());
            return Optional.empty();
        }
    }
}
