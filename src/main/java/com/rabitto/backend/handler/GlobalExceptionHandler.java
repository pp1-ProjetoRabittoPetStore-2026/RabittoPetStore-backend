package com.rabitto.backend.handler;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Map<String, String> CONSTRAINT_MESSAGES = Map.of(
        "tutors_email_key", "Este email já está cadastrado",
        "tutors_telefone_key", "Este telefone já está cadastrado",
        "funcionarios_cpf_key", "Este CPF já está cadastrado"
    );

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        for (var entry : CONSTRAINT_MESSAGES.entrySet()) {
            if (message.contains(entry.getKey())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", entry.getValue()));
            }
        }
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Já existe um registro com este valor"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
    }
}