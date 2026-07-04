package com.gomatch.client.ui;

import javax.swing.*;
import java.awt.*;

public class BoardUI extends JFrame {
    private static final int SIZE = 15;
    private static final int CELL_SIZE = 36;
    private static final int BOARD_SIZE = (SIZE - 1) * CELL_SIZE + 40;

    public BoardUI() {
        setTitle("Gomatch 对局");
        setSize(BOARD_SIZE + 200, BOARD_SIZE + 40);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        BoardPanel boardPanel = new BoardPanel();
        add(boardPanel, java.awt.BorderLayout.CENTER);

        JPanel sidePanel = new JPanel(new java.awt.BorderLayout());
        sidePanel.setPreferredSize(new Dimension(200, BOARD_SIZE));
        sidePanel.add(new JLabel("聊天", SwingConstants.CENTER), java.awt.BorderLayout.NORTH);
        sidePanel.add(new JScrollPane(new JTextArea(10, 20)), java.awt.BorderLayout.CENTER);
        add(sidePanel, java.awt.BorderLayout.EAST);
    }

    static class BoardPanel extends JPanel {
        public BoardPanel() {
            setPreferredSize(new Dimension(BOARD_SIZE, BOARD_SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int margin = 20;
            for (int i = 0; i < SIZE; i++) {
                g.drawLine(margin, margin + i * CELL_SIZE, margin + (SIZE - 1) * CELL_SIZE, margin + i * CELL_SIZE);
                g.drawLine(margin + i * CELL_SIZE, margin, margin + i * CELL_SIZE, margin + (SIZE - 1) * CELL_SIZE);
            }
        }
    }
}
