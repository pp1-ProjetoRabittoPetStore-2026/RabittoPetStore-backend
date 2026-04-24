package com.rabitto.backend.models;

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
    private String cargo;

    @Column(unique = true)
    private String cpf;

    private String telefone;
    private String senha;

    // Define se o funcionário ainda trabalha na loja
    private Boolean ativo = true;
}