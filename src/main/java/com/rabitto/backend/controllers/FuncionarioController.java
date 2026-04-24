package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.repositories.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository repository;

    @GetMapping
    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    @PostMapping
    public Funcionario salvar(@RequestBody Funcionario funcionario) {
        return repository.save(funcionario);
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable Long id, @RequestBody Funcionario funcionarioAtualizado) {
        funcionarioAtualizado.setId(id);
        return repository.save(funcionarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public void desativar(@PathVariable Long id) {
        // Busca o cara no banco
        Funcionario funcionario = repository.findById(id).orElse(null);

        if (funcionario != null) {
            // Em vez de apagar, a gente só diz que ele não tá mais ativo
            funcionario.setAtivo(false);
            repository.save(funcionario);
        }
    }
}