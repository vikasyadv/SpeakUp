package com.speakup.model;

public enum Stance {
    FOR,
    AGAINST;

    public static Stance fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Stance.valueOf(value.trim().toUpperCase());
    }
}
