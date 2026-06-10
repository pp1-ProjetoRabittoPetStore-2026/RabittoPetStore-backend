package com.rabitto.backend.repositories;

import com.rabitto.backend.models.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FuncionarioRepository extends JpaRepository<Funcionario, Long> {
    Optional<Funcionario> findByCpf(String cpf);

    Optional<Funcionario> findByEmail(String email);

    List<Funcionario> findByAtivoTrue();

    List<Funcionario> findByCargoIgnoreCaseAndAtivoTrue(String cargo);
}
