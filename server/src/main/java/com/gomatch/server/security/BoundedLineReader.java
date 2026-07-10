package com.gomatch.server.security;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;

/** Reads newline-delimited protocol frames without allowing unbounded lines. */
public final class BoundedLineReader {
    private final Reader reader;
    private final int maxChars;

    public BoundedLineReader(Reader reader, int maxChars) {
        this.reader = Objects.requireNonNull(reader, "reader");
        if (maxChars < 1) {
            throw new IllegalArgumentException("maxChars must be positive");
        }
        this.maxChars = maxChars;
    }

    public String readLine() throws IOException {
        StringBuilder line = new StringBuilder(Math.min(maxChars, 1024));
        boolean pendingCarriageReturn = false;
        while (true) {
            int value = reader.read();
            if (value == -1) {
                if (pendingCarriageReturn) {
                    appendChecked(line, '\r');
                }
                return line.isEmpty() ? null : line.toString();
            }
            if (value == '\n') {
                return line.toString();
            }
            if (pendingCarriageReturn) {
                appendChecked(line, '\r');
                pendingCarriageReturn = false;
            }
            if (value == '\r') {
                pendingCarriageReturn = true;
            } else {
                appendChecked(line, (char) value);
            }
        }
    }

    private void appendChecked(StringBuilder line, char value) throws LineTooLongException {
        if (line.length() >= maxChars) {
            throw new LineTooLongException(maxChars);
        }
        line.append(value);
    }

    public static final class LineTooLongException extends IOException {
        private final int maxChars;

        public LineTooLongException(int maxChars) {
            super("Protocol line exceeds " + maxChars + " characters");
            this.maxChars = maxChars;
        }

        public int getMaxChars() {
            return maxChars;
        }
    }
}
