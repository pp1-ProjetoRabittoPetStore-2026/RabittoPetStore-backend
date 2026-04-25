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
    private String porte;
    private String especie; // Ex: Cachorro, Gato, Calopsita
    private Integer idade;  // Idade em anos (ou meses String)

    @ManyToOne
    @JoinColumn(name = "tutor_id")
    private Tutor tutor;
}