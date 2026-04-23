package com.rabitto.backend.models;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "pets")
@Data
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nome;

    private String raca;

    private String porte; // Pequeno, Médio, Grande


    @ManyToOne
    @JoinColumn(name = "tutor_id") // Vai criar uma coluna no banco guardando o ID do dono
    private Tutor tutor;
}