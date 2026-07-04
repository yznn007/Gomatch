package com.gomatch.client.ui;

import javax.swing.*;

public class LobbyUI extends JFrame {
    private JTable roomTable;

    public LobbyUI() {
        setTitle("Gomatch 游戏大厅");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        roomTable = new JTable(new String[][]{}, new String[]{"房间号", "房间名", "房主", "人数", "状态"});
        add(new JScrollPane(roomTable), java.awt.BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        btnPanel.add(new JButton("创建房间"));
        btnPanel.add(new JButton("加入房间"));
        btnPanel.add(new JButton("刷新"));
        btnPanel.add(new JButton("排行榜"));
        add(btnPanel, java.awt.BorderLayout.SOUTH);
    }
}
