package com.rabitto.backend.models;

public enum Porte {
    PEQUENO("Pequeno"),
    MEDIO("Médio"),
    GRANDE("Grande");

    private final String displayName;

    Porte(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
