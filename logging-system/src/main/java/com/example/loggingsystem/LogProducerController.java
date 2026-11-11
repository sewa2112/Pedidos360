package com.example.loggingsystem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin(originPatterns = "*")
public class LogProducerController {

    private final RabbitTemplate rabbitTemplate;

    public LogProducerController(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("/log")
    public ResponseEntity<Map<String, String>> sendLog(@Valid @RequestBody LogMessage logMessage) {
        String normalizedLevel = logMessage.getLevel().trim().toUpperCase();
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                normalizedLevel,
                logMessage.getMessage().trim()
        );

        return ResponseEntity.ok(Map.of(
                "status", "Log enviado",
                "level", normalizedLevel,
                "message", logMessage.getMessage().trim()
        ));
    }

    @GetMapping("/api/logs/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "service", "logging-system",
                "status", "Online",
                "exchange", RabbitMQConfig.EXCHANGE_NAME,
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400,
                "error", "Validacion fallida",
                "errors", errors
        ));
    }

    public static class LogMessage {

        @NotBlank(message = "El nivel de log no puede estar vacio")
        @Pattern(
                regexp = "^(?i)(INFO|WARNING|ERROR)$",
                message = "El nivel debe ser INFO, WARNING o ERROR"
        )
        private String level;

        @NotBlank(message = "El mensaje de log no puede estar vacio")
        @Size(max = 500, message = "El mensaje no puede superar los 500 caracteres")
        private String message;

        public LogMessage() {}

        public LogMessage(String level, String message) {
            this.level = level;
            this.message = message;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
