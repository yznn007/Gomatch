package com.gomatch.client.network;

import com.gomatch.common.Protocol;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerConnector {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private volatile boolean running;
    private Thread receiveThread;
    private final List<NetworkListener> listeners = new CopyOnWriteArrayList<>();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        startReceiveThread();
    }

    public void send(String... parts) {
        if (writer != null) {
            writer.println(Protocol.build(parts));
        }
    }

    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }

    public void disconnect() {
        running = false;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        listeners.clear();
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void startReceiveThread() {
        running = true;
        receiveThread = new Thread(() -> {
            while (running && isConnected()) {
                try {
                    String line = reader.readLine();
                    if (line == null) break;
                    String[] parts = Protocol.parse(line);
                    String cmd = parts[0];
                    String[] args = Arrays.copyOfRange(parts, 1, parts.length);
                    Platform.runLater(() -> {
                        for (NetworkListener l : listeners) {
                            l.onMessage(cmd, args);
                        }
                    });
                } catch (IOException e) {
                    break;
                }
            }
            Platform.runLater(() -> {
                for (NetworkListener l : listeners) {
                    l.onMessage("DISCONNECT", new String[0]);
                }
            });
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
}
