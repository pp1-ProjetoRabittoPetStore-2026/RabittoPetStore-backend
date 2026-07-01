package com.rabitto.backend.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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
                log.warn("Violacao de constraint: {}", entry.getKey());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", entry.getValue()));
            }
        }
        log.warn("Violacao de integridade nao mapeada: {}", message);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "Já existe um registro com este valor"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        if (ex.getStatusCode().is5xxServerError()) {
            log.error("Erro de servidor: status={} reason={}", ex.getStatusCode(), ex.getReason(), ex);
        } else {
            log.warn("Requisicao rejeitada: status={} reason={}", ex.getStatusCode(), ex.getReason());
        }
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String required = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "?";
        log.warn("Parametro invalido: nome={} valor={} tipoEsperado={}", ex.getName(), ex.getValue(), required);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Parâmetro '" + ex.getName() + "' inválido, esperado " + required));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("Erro interno nao tratado: {}", ex.getClass().getSimpleName(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro interno do servidor"));
    }
}