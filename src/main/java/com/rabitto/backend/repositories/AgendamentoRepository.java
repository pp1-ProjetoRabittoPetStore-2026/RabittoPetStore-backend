package com.rabitto.backend.repositories;

import com.rabitto.backend.models.Agendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    

    List<Agendamento> findByDataHora(LocalDateTime dataHora);

    

    List<Agendamento> findByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    List<Agendamento> findByDataHoraBetweenOrderByDataHoraAsc(LocalDateTime inicio, LocalDateTime fim);

    

    List<Agendamento> findByStatus(String status);

    List<Agendamento> findByStatusOrderByDataHoraAsc(String status);

    List<Agendamento> findAllByOrderByDataHoraAsc();

    

    List<Agendamento> findByFuncionariosIdAndDataHoraBetweenOrderByDataHoraAsc(
            Long funcionarioId, LocalDateTime inicio, LocalDateTime fim);

    

    List<Agendamento> findByPetTutorIdOrderByDataHoraAsc(Long tutorId);



    List<Agendamento> findByPetIdAndDataHoraAndServicosIdIn(
            Long petId, LocalDateTime dataHora, List<Long> servicoIds);
}
