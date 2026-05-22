package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Pet;
import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.PetRepository;
import com.rabitto.backend.repositories.TutorRepository;
import com.rabitto.backend.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetRepository repository;

    @Autowired
    private TutorRepository tutorRepository;

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public List<Pet> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/me")
    public List<Pet> listarDoTutorLogado(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = jwtService.extractTutorId(authorizationHeader);
        return repository.findByTutorId(tutorId);
    }

    @PostMapping
    public Pet salvar(
            @RequestBody Pet pet,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = jwtService.extractTutorId(authorizationHeader);
        Tutor tutor = tutorRepository.findById(tutorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        pet.setTutor(tutor);
        return repository.save(pet);
    }

    @GetMapping("/{id}")
    public Pet buscarPorId(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = jwtService.extractTutorId(authorizationHeader);
        Pet pet = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));
        if (!pet.getTutor().getId().equals(tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este pet não pertence ao tutor logado");
        }
        return pet;
    }

    @PutMapping("/{id}")
    public Pet atualizar(
            @PathVariable Long id,
            @RequestBody Pet petAtualizado,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = jwtService.extractTutorId(authorizationHeader);
        Pet existing = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));
        if (!existing.getTutor().getId().equals(tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este pet não pertence ao tutor logado");
        }
        petAtualizado.setId(id);
        petAtualizado.setTutor(existing.getTutor());
        return repository.save(petAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = jwtService.extractTutorId(authorizationHeader);
        Pet pet = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pet não encontrado"));
        if (!pet.getTutor().getId().equals(tutorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Este pet não pertence ao tutor logado");
        }
        repository.deleteById(id);
    }
}
