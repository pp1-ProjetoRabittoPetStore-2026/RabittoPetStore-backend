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
}