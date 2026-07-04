package com.gomatch.client.network;

import com.gomatch.common.Protocol;
import java.io.*;
import java.net.Socket;

public class ServerConnector {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
    }

    public void send(String... parts) {
        writer.println(Protocol.build(parts));
    }

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public void disconnect() {
        try { socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }
}
