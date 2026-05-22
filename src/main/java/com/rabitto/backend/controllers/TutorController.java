package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.TutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/tutores")
public class TutorController {

    @Autowired
    private TutorRepository repository;

    @GetMapping
    public List<Tutor> listar() {
        return repository.findAll();
    }

    @PostMapping
    public Tutor salvar(@RequestBody Tutor tutor) {
        if (repository.findByEmail(tutor.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está cadastrado");
        }
        if (repository.findByTelefone(tutor.getTelefone()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este telefone já está cadastrado");
        }
        return repository.save(tutor);
    }

    @PutMapping("/{id}")
    public Tutor atualizar(@PathVariable Long id, @RequestBody Tutor tutorAtualizado) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        tutorAtualizado.setId(id);
        repository.findByEmail(tutorAtualizado.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está cadastrado");
            }
        });
        repository.findByTelefone(tutorAtualizado.getTelefone()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este telefone já está cadastrado");
            }
        });
        return repository.save(tutorAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado");
        }
        repository.deleteById(id);
    }
}
