package com.gomatch.client.audio;

import com.gomatch.client.I18N;

public enum PlayOrder {
    SINGLE_LOOP("playorder.single", "⟳"),
    SEQUENTIAL("playorder.sequential", "▶"),
    RANDOM("playorder.random", "⇄");

    private final String i18nKey;
    private final String symbol;

    PlayOrder(String i18nKey, String symbol) {
        this.i18nKey = i18nKey;
        this.symbol = symbol;
    }

    public String getDisplayName() {
        return I18N.get(i18nKey);
    }

    public String getSymbol() {
        return symbol;
    }

    public static PlayOrder fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return SINGLE_LOOP;
        }
    }
}
