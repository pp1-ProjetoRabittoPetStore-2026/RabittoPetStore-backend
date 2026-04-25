package com.rabitto.backend.repositories;

import com.rabitto.backend.models.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    // faz o "SELECT" no banco pela DataHora automaticamente!
    List<Agendamento> findByDataHora(LocalDateTime dataHora);

    // O Spring vai buscar tudo que estiver "entre" o começo e o fim do dia
    List<Agendamento> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    // O Spring gera a query automaticamente com base no nome do método
    List<Agendamento> findByStatus(String status);
}