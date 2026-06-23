package com.rabitto.backend.security;

import java.util.Set;


public final class Roles {

    public static final String TUTOR = "TUTOR";
    public static final String GERENTE = "GERENTE";
    public static final String CAIXA = "CAIXA";
    public static final String TOSADOR = "TOSADOR";
    public static final String VETERINARIO = "VETERINARIO";

    
    public static final Set<String> STAFF = Set.of(GERENTE, CAIXA, TOSADOR, VETERINARIO);

    private Roles() {
    }
}
