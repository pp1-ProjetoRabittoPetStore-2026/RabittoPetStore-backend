package com.rabitto.backend.security;

import java.util.Set;

/**
 * Papeis de acesso do sistema. Os codigos sao gravados no claim "role" do JWT
 * e no campo Funcionario.cargo (exceto TUTOR, que vem da tabela de tutores).
 */
public final class Roles {

    public static final String TUTOR = "TUTOR";
    public static final String GERENTE = "GERENTE";
    public static final String CAIXA = "CAIXA";
    public static final String TOSADOR = "TOSADOR";
    public static final String VETERINARIO = "VETERINARIO";

    /** Qualquer funcionario (nao-tutor). */
    public static final Set<String> STAFF = Set.of(GERENTE, CAIXA, TOSADOR, VETERINARIO);

    private Roles() {
    }
}
