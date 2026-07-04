package com.gomatch.client.network;

public interface NetworkListener {
    void onMessage(String command, String[] args);
}
