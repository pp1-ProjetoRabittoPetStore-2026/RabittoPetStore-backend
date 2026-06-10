package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Tutor;
import com.rabitto.backend.repositories.TutorRepository;
import com.rabitto.backend.services.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/tutores")
public class TutorController {

    @Autowired
    private TutorRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @GetMapping
    public List<Tutor> listar() {
        List<Tutor> tutores = repository.findAll();
        tutores.forEach(t -> t.setSenha(null));
        return tutores;
    }

    @PostMapping
    public Tutor salvar(@RequestBody Tutor tutor) {
        validarUnicidade(tutor, null);
        tutor.setSenha(passwordEncoder.encode(tutor.getSenha()));
        Tutor salvo = repository.save(tutor);
        salvo.setSenha(null);
        return salvo;
    }

    // ----- Perfil do proprio tutor logado -----
    @GetMapping("/me")
    public Tutor meuPerfil(@RequestHeader(value = "Authorization", required = false) String header) {
        Long tutorId = jwtService.extractTutorId(header);
        Tutor tutor = repository.findById(tutorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        tutor.setSenha(null);
        return tutor;
    }

    @PutMapping("/me")
    public Tutor atualizarMeuPerfil(
            @RequestHeader(value = "Authorization", required = false) String header,
            @RequestBody Tutor dados) {
        Long tutorId = jwtService.extractTutorId(header);
        return atualizarInterno(tutorId, dados);
    }

    @PutMapping("/{id}")
    public Tutor atualizar(@PathVariable Long id, @RequestBody Tutor tutorAtualizado) {
        return atualizarInterno(id, tutorAtualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado");
        }
        repository.deleteById(id);
    }

    private Tutor atualizarInterno(Long id, Tutor dados) {
        Tutor existente = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tutor não encontrado"));
        validarUnicidade(dados, id);

        existente.setNome(dados.getNome());
        existente.setEmail(dados.getEmail());
        existente.setTelefone(dados.getTelefone());
        // So troca a senha se vier preenchida
        if (dados.getSenha() != null && !dados.getSenha().isBlank()) {
            existente.setSenha(passwordEncoder.encode(dados.getSenha()));
        }

        Tutor salvo = repository.save(existente);
        salvo.setSenha(null);
        return salvo;
    }

    private void validarUnicidade(Tutor tutor, Long ignoreId) {
        repository.findByEmail(tutor.getEmail()).ifPresent(existing -> {
            if (!existing.getId().equals(ignoreId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este email já está cadastrado");
            }
        });
        repository.findByTelefone(tutor.getTelefone()).ifPresent(existing -> {
            if (!existing.getId().equals(ignoreId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Este telefone já está cadastrado");
            }
        });
    }
}
