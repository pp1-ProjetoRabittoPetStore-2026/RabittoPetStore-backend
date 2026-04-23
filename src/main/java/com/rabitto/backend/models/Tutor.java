package com.rabitto.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "tutors")
@Data
public class Tutor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    @Column(unique = true)
    private String email;

    private String senha;

    @Column(unique = true)
    private String telefone;
}