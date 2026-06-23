package com.rabitto.backend.models;

public enum Especie {
    CACHORRO("Cachorro"),
    GATO("Gato"),
    PASSARO("Pássaro"),
    PEIXE("Peixe"),
    ROEDOR("Roedor"),
    COELHO("Coelho"),
    REPTIL("Réptil"),
    FURAO("Furão"),
    OURICO("Ouriço"),
    MINI_PIG("Mini Pig");

    private final String displayName;

    Especie(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
