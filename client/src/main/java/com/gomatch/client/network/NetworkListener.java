package com.gomatch.client.network;

import com.gomatch.common.Message;

public interface NetworkListener {
    void onMessage(Message msg);
}
