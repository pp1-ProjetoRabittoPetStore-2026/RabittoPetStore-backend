package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.TutorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tutores")
public class TutorController {

    @Autowired
    private TutorRepository repository;

    @GetMapping // Endpoint para listar todo mundo: GET /tutores
    public List<Tutor> listar() {
        return repository.findAll();
    }

    @PostMapping // Endpoint to register: POST /tutores
    public Tutor save(@RequestBody Tutor tutor) {
        return repository.save(tutor);
    }

    @PutMapping("/{id}")
    public Tutor atualizar(@PathVariable Long id, @RequestBody Tutor tutorAtualizado) {
        tutorAtualizado.setId(id);
        return repository.save(tutorAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
    }
}