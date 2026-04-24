package com.rabitto.backend.controllers;

import com.rabitto.backend.models.Pet;
import com.rabitto.backend.repositories.PetRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/pets")
public class PetController {

    @Autowired
    private PetRepository repository;

    @Value("${app.auth.jwt-secret:change-this-secret-key-with-at-least-32-characters}")
    private String jwtSecret;

    @GetMapping
    public List<Pet> listarTodos() {
        return repository.findAll();
    }

    @GetMapping("/me")
    public List<Pet> listarDoTutorLogado(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        Long tutorId = extractTutorIdFromToken(authorizationHeader);
        return repository.findByTutorId(tutorId);
    }

    @PostMapping
    public Pet salvar(@RequestBody Pet pet) {
        return repository.save(pet);
    }

    @PutMapping("/{id}") // A URL agora vai pedir o ID do pet: PUT /pets/1
    public Pet atualizar(@PathVariable Long id, @RequestBody Pet petAtualizado) {
        // pega o ID que veio na URL e coloca no pet que veio no JSON
        petAtualizado.setId(id);
        // Como o pet já tem um ID, o Spring sabe que é pra sobrescrever e não criar um
        // novo
        return repository.save(petAtualizado);
    }

    @DeleteMapping("/{id}") // A URL também pede o ID: DELETE /pets/1
    public void deletar(@PathVariable Long id) {
        // Vai lá no banco e apaga o pet pelo ID dele
        repository.deleteById(id);
    }

    private Long extractTutorIdFromToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de acesso ausente");
        }

        String token = authorizationHeader.substring(7);

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Number tutorId = claims.get("tutorId", Number.class);
            if (tutorId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }

            return tutorId.longValue();
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido", ex);
        }
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}