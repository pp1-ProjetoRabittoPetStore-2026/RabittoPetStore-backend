package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Agendamento;
import com.rabitto.backend.models.Funcionario;
import com.rabitto.backend.repositories.AgendamentoRepository;
import com.rabitto.backend.repositories.FuncionarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/funcionarios")
public class FuncionarioController {

    @Autowired
    private FuncionarioRepository repository;

    @Autowired
    private AgendamentoRepository agendamentoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    public List<Funcionario> listarTodos() {
        List<Funcionario> todos = repository.findAll();
        todos.forEach(f -> f.setSenha(null));
        return todos;
    }

    @PostMapping
    public Funcionario salvar(@RequestBody Funcionario funcionario) {
        if (funcionario.getCpf() != null && repository.findByCpf(funcionario.getCpf()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado");
        }
        if (funcionario.getEmail() != null && repository.findByEmail(funcionario.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está cadastrado");
        }
        if (funcionario.getSenha() != null && !funcionario.getSenha().isBlank()) {
            funcionario.setSenha(passwordEncoder.encode(funcionario.getSenha()));
        }
        Funcionario salvo = repository.save(funcionario);
        salvo.setSenha(null);
        return salvo;
    }

    @PutMapping("/{id}")
    public Funcionario atualizar(@PathVariable Long id, @RequestBody Funcionario dados) {
        Funcionario existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));

        if (dados.getCpf() != null) {
            repository.findByCpf(dados.getCpf()).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este CPF já está cadastrado");
                }
            });
        }
        if (dados.getEmail() != null) {
            repository.findByEmail(dados.getEmail()).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está cadastrado");
                }
            });
        }

        existente.setNome(dados.getNome());
        existente.setCargo(dados.getCargo());
        existente.setCpf(dados.getCpf());
        existente.setEmail(dados.getEmail());
        existente.setTelefone(dados.getTelefone());
        if (dados.getAtivo() != null) {
            existente.setAtivo(dados.getAtivo());
        }
        

        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            existente.setSenha(passwordEncoder.encode(dados.getSenha()));
        }

        Funcionario salvo = repository.save(existente);
        salvo.setSenha(null);
        return salvo;
    }

    @DeleteMapping("/{id}")
    public void desativar(@PathVariable Long id) {
        Funcionario funcionario = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado"));
        funcionario.setAtivo(false);
        repository.save(funcionario);
    }

    

    @GetMapping("/agenda")
    public List<Map<String, Object>> agenda(
            @RequestParam(required = false) LocalDate data,
            @RequestParam(required = false) String cargo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String nome) {
        LocalDate dia = data != null ? data : LocalDate.now();
        LocalDateTime inicio = dia.atTime(0, 0);
        LocalDateTime fim = dia.atTime(23, 59);

        List<Agendamento> agendamentosDoDia =
                agendamentoRepository.findByDataHoraBetweenOrderByDataHoraAsc(inicio, fim);

        String nomeBusca = nome == null ? "" : nome.trim().toLowerCase();

        return repository.findByAtivoTrue().stream()
                .filter(f -> isVet(f.getCargo()) || isTosador(f.getCargo()))
                .filter(f -> matchesCargo(f.getCargo(), cargo))
                .filter(f -> nomeBusca.isEmpty()
                        || (f.getNome() != null && f.getNome().toLowerCase().contains(nomeBusca)))
                .map(func -> {
                    List<Agendamento> agendamentos = agendamentosDoDia.stream()
                            .filter(a -> a.getFuncionarios().stream()
                                    .anyMatch(f -> Objects.equals(f.getId(), func.getId())))
                            .filter(a -> status == null || status.isBlank()
                                    || status.equalsIgnoreCase(a.getStatus()))
                            .peek(a -> a.getFuncionarios().forEach(f -> f.setSenha(null)))
                            .toList();

                    func.setSenha(null);
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("funcionario", func);
                    item.put("agendamentos", agendamentos);
                    return item;
                })
                .toList();
    }

    private boolean matchesCargo(String cargo, String filtro) {
        if (filtro == null || filtro.isBlank()) {
            return true;
        }
        String f = filtro.toUpperCase();
        if (f.contains("VETERIN")) {
            return isVet(cargo);
        }
        if (f.contains("TOSAD") || f.contains("BANHIST")) {
            return isTosador(cargo);
        }
        return true;
    }

    private boolean isVet(String cargo) {
        return cargo != null && cargo.toUpperCase().contains("VETERIN");
    }

    private boolean isTosador(String cargo) {
        String c = cargo == null ? "" : cargo.toUpperCase();
        return c.contains("TOSAD") || c.contains("BANHIST");
    }
}
