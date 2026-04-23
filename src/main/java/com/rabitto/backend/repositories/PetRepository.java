package com.rabitto.backend.repositories;

import com.rabitto.backend.models.Pet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {
    // Esse comando aqui faz a listagem dos pets de um dono específico
    List<Pet> findByTutorId(Long tutorId);
}