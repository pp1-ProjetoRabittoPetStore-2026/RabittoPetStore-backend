package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Servico;
import com.rabitto.backend.repositories.ServicoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/servicos")
public class ServicoController {

    private static final Logger log = LoggerFactory.getLogger(ServicoController.class);

    @Autowired
    private ServicoRepository repository;

    @GetMapping
    public List<Servico> listarTodos() {
        return repository.findAll();
    }

    @PostMapping
    public Servico salvar(@RequestBody Servico servico) {
        Servico salvo = repository.save(servico);
        log.info("Servico criado: id={} nome={}", salvo.getId(), salvo.getNome());
        return salvo;
    }

    @PutMapping("/{id}")
    public Servico atualizar(@PathVariable Long id, @RequestBody Servico servicoAtualizado) {
        servicoAtualizado.setId(id);
        Servico salvo = repository.save(servicoAtualizado);
        log.info("Servico atualizado: id={}", id);
        return salvo;
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        repository.deleteById(id);
        log.info("Servico removido: id={}", id);
    }
}