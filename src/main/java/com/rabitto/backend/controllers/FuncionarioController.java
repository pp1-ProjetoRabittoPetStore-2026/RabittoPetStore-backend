package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository repository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @GetMapping
    public List<Funcionario> listarTodos() {
        return repository.findAll();
    }

    @PostMapping
    public Funcionario salvar(@RequestBody Funcionario funcionario) {
        if (repository.findByCpf(funcionario.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado");
        }
        return repository.save(funcionario);
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable Long id, @RequestBody Funcionario funcionarioAtualizado) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
        funcionarioAtualizado.setId(id);
        repository.findByCpf(funcionarioAtualizado.getCpf()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado");
            }
        });
        return repository.save(funcionarioAtualizado);
    }

    @DeleteMapping("/{id}")
    public void desativar(@PathVariable Long id) {
        Funcionario funcionario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
        funcionario.setAtivo(false);
        repository.save(funcionario);
    }

    // Retorna cada funcionário ativo (vet/banhista) com seus agendamentos do dia
    @GetMapping("/agenda")
    public List<Map<String, Object>> agenda(@RequestParam(required = false) LocalDate data) {
        LocalDate dia = data != null ? data : LocalDate.now();
        LocalDateTime inicio = dia.atTime(9, 0);
        LocalDateTime fim = dia.atTime(17, 0);

        List<Agendamento> agendamentosDoDia = agendamentoRepository.findByDataHoraBetween(inicio, fim);

        return repository.findAll().stream()
                .filter(f -> Boolean.TRUE.equals(f.getAtivo()))
                .filter(f -> f.getCargo() != null && (
                        f.getCargo().toLowerCase().contains("veterinário") ||
                        f.getCargo().toLowerCase().contains("veterinario") ||
                        f.getCargo().toLowerCase().contains("banhista")))
                .map(func -> {
                    boolean isVet = func.getCargo().toLowerCase().contains("veterinário") ||
                                    func.getCargo().toLowerCase().contains("veterinario");

                    List<Agendamento> agendamentos = agendamentosDoDia.stream()
                            .filter(a -> {
                                String nomeServico = a.getServico().getNome().toLowerCase();
                                boolean agendVet = nomeServico.contains("consulta") || nomeServico.contains("vacina");
                                return isVet == agendVet;
                            })
                            .toList();

                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("funcionario", func);
                    item.put("agendamentos", agendamentos);
                    return item;
                })
                .toList();
    }
}
