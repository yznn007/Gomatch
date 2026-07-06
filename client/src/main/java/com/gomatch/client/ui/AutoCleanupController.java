package com.gomatch.client.ui;

import com.gomatch.client.ClientMain;
import com.gomatch.client.network.NetworkListener;

/**
 * Controller 基类，自动管理 NetworkListener 的注册与注销。
 * 子类无需手动调用 addListener/removeListener。
 */
public abstract class AutoCleanupController implements NetworkListener {

    private boolean registered = false;

    /**
     * 注册当前实例为 NetworkListener，可安全多次调用（幂等）。
     */
    protected void registerListener() {
        if (!registered) {
            ClientMain.getConnector().addListener(this);
            registered = true;
        }
    }

    /**
     * 注销当前实例的 NetworkListener，可安全多次调用（幂等）。
     */
    protected void unregisterListener() {
        if (registered) {
            ClientMain.getConnector().removeListener(this);
            registered = false;
        }
    }

    /**
     * 供 ScreenManager 在页面切换时调用，清理旧 controller。
     */
    public void cleanup() {
        unregisterListener();
    }
}
