package com.gomatch.client.audio;

public enum PlayOrder {
    SINGLE_LOOP("单曲循环", "⟳"),
    SEQUENTIAL("顺序播放", "▶"),
    RANDOM("随机播放", "⇄");

    private final String displayName;
    private final String symbol;

    PlayOrder(String displayName, String symbol) {
        this.displayName = displayName;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSymbol() {
        return symbol;
    }

    public static PlayOrder fromDisplayName(String name) {
        for (PlayOrder order : values()) {
            if (order.displayName.equals(name)) {
                return order;
            }
        }
        return SINGLE_LOOP;
    }
}
