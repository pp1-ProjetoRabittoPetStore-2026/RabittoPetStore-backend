package com.rabitto.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos")
@Data
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private LocalDateTime dataHora;

    // o agendamento do Pet que vai receber o serviço
    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    // o agendamento do Serviço escolhido (Banho, Tosa, etc.)
    @ManyToOne
    @JoinColumn(name = "servico_id")
    private Servico servico;
}