package com.rabitto.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "funcionarios")
@Data
public class Funcionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    // Papel/cargo: GERENTE, CAIXA, TOSADOR, VETERINARIO (ver Roles)
    private String cargo;

    @Column(unique = true)
    private String cpf;

    // Email de acesso ao back-office (login de funcionario)
    @Column(unique = true)
    private String email;

    private String telefone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    // Define se o funcionário ainda trabalha na loja
    private Boolean ativo = true;
}