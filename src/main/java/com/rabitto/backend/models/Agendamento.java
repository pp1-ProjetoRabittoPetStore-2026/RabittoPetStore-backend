package com.rabitto.backend.models;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agendamentos")
@Data
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime dataHora;

    // Começa como "Pendente" por padrão
    private String status = "Pendente";

    @ManyToOne
    @JoinColumn(name = "pet_id")
    private Pet pet;

    // Multiplos servicos por agendamento (ex: Consulta + Banho).
    @ManyToMany
    @JoinTable(
            name = "agendamento_servicos",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "servico_id"))
    private List<Servico> servicos = new ArrayList<>();

    // Profissionais designados: um por setor presente nos servicos
    // (ex: um veterinario + um tosador). Previne sobreposicao por profissional.
    @ManyToMany
    @JoinTable(
            name = "agendamento_funcionarios",
            joinColumns = @JoinColumn(name = "agendamento_id"),
            inverseJoinColumns = @JoinColumn(name = "funcionario_id"))
    private List<Funcionario> funcionarios = new ArrayList<>();
}
