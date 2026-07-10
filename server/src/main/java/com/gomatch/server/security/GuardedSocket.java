package com.gomatch.server.security;

import com.gomatch.common.Message;
import com.gomatch.common.Protocol;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Delegates a socket while filtering newline-delimited protocol traffic. */
public final class GuardedSocket extends Socket {
    private final Socket delegate;
    private final SafeProtocolOutputStream guardedOutput;
    private final InputStream guardedInput;
    private final Runnable releaseConnection;
    private final AtomicBoolean closed = new AtomicBoolean();

    public GuardedSocket(Socket delegate, ProtocolRequestGuard requestGuard, Runnable releaseConnection)
            throws IOException {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.releaseConnection = Objects.requireNonNull(releaseConnection, "releaseConnection");
        this.guardedOutput = new SafeProtocolOutputStream(delegate.getOutputStream());
        this.guardedInput = new GuardedProtocolInputStream(delegate.getInputStream(), requestGuard,
                delegate.getInetAddress(), guardedOutput);
    }

    @Override public InputStream getInputStream() { return guardedInput; }
    @Override public OutputStream getOutputStream() { return guardedOutput; }
    @Override public InetAddress getInetAddress() { return delegate.getInetAddress(); }
    @Override public InetAddress getLocalAddress() { return delegate.getLocalAddress(); }
    @Override public int getPort() { return delegate.getPort(); }
    @Override public int getLocalPort() { return delegate.getLocalPort(); }
    @Override public SocketAddress getRemoteSocketAddress() { return delegate.getRemoteSocketAddress(); }
    @Override public boolean isConnected() { return delegate.isConnected(); }
    @Override public boolean isClosed() { return closed.get() || delegate.isClosed(); }

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            try {
                delegate.close();
            } finally {
                releaseConnection.run();
            }
        }
    }

    private static final class GuardedProtocolInputStream extends InputStream {
        private final BoundedLineReader reader;
        private final ProtocolRequestGuard requestGuard;
        private final InetAddress address;
        private final SafeProtocolOutputStream output;
        private byte[] current = new byte[0];
        private int offset;

        private GuardedProtocolInputStream(InputStream input, ProtocolRequestGuard requestGuard,
                                           InetAddress address, SafeProtocolOutputStream output) {
            this.reader = new BoundedLineReader(new InputStreamReader(input, StandardCharsets.UTF_8),
                    SecurityLimits.MAX_MESSAGE_CHARS);
            this.requestGuard = requestGuard;
            this.address = address;
            this.output = output;
        }

        @Override
        public int read() throws IOException {
            if (!ensureCurrent()) {
                return -1;
            }
            return current[offset++] & 0xff;
        }

        @Override
        public int read(byte[] target, int targetOffset, int length) throws IOException {
            Objects.checkFromIndexSize(targetOffset, length, target.length);
            if (length == 0) {
                return 0;
            }
            if (!ensureCurrent()) {
                return -1;
            }
            int copied = Math.min(length, current.length - offset);
            System.arraycopy(current, offset, target, targetOffset, copied);
            offset += copied;
            return copied;
        }

        private boolean ensureCurrent() throws IOException {
            if (offset < current.length) {
                return true;
            }
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    return false;
                }
                try {
                    String normalized = requestGuard.validateAndNormalize(line, address);
                    current = (normalized + '\n').getBytes(StandardCharsets.UTF_8);
                    offset = 0;
                    return true;
                } catch (ProtocolRequestGuard.ProtocolRejectedException rejected) {
                    output.writeMessage(Message.of(Protocol.ERROR, "msg", "请求被安全策略拒绝").toJson());
                }
            }
        }
    }

    /** Ensures a malformed server response can never terminate the client parser. */
    private static final class SafeProtocolOutputStream extends OutputStream {
        private final OutputStream delegate;
        private final ByteArrayOutputStream pending = new ByteArrayOutputStream();

        private SafeProtocolOutputStream(OutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public synchronized void write(int value) throws IOException {
            if (value == '\n') {
                flushLine();
            } else if (pending.size() < SecurityLimits.MAX_MESSAGE_CHARS) {
                pending.write(value);
            } else {
                pending.reset();
                writeMessage(Message.of(Protocol.ERROR, "msg", "响应过大").toJson());
            }
        }

        @Override
        public synchronized void write(byte[] bytes, int offset, int length) throws IOException {
            Objects.checkFromIndexSize(offset, length, bytes.length);
            for (int index = offset; index < offset + length; index++) {
                write(bytes[index] & 0xff);
            }
        }

        @Override
        public synchronized void flush() throws IOException {
            delegate.flush();
        }

        private void flushLine() throws IOException {
            String line = pending.toString(StandardCharsets.UTF_8);
            pending.reset();
            try {
                new JSONObject(line);
                writeMessage(line);
            } catch (RuntimeException malformed) {
                writeMessage(Message.of(Protocol.ERROR, "msg", "响应包含无效数据").toJson());
            }
        }

        private void writeMessage(String message) throws IOException {
            delegate.write(message.getBytes(StandardCharsets.UTF_8));
            delegate.write('\n');
            delegate.flush();
        }
    }
}
