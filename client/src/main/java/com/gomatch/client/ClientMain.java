package com.gomatch.client;

import com.gomatch.client.ui.LoginUI;
import javax.swing.*;

public class ClientMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginUI().setVisible(true));
    }
}
