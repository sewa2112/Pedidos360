package com.example.controller;

import com.example.dto.CreateBindingDTO;
import com.example.dto.CreateExchangeDTO;
import com.example.dto.CreateQueueDTO;
import com.example.service.RabbitResourceManager;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping({"/api/rabbitmq", ""})
@CrossOrigin(originPatterns = "*")
public class RabbitMQController {

    private static final Pattern RESOURCE_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]+$");
    private static final Pattern EXCHANGE_TYPE_PATTERN = Pattern.compile("^(direct|topic|fanout|headers)$");
    private static final Pattern ROUTING_KEY_PATTERN = Pattern.compile("^[a-zA-Z0-9._*#-]+$");

    private final RabbitResourceManager resourceManager;

    public RabbitMQController(RabbitResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    private String validateResourceName(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de " + label + " no puede estar vacío");
        }
        String trimmed = value.trim();
        if (trimmed.length() < 3 || trimmed.length() > 50) {
            throw new IllegalArgumentException("El nombre de " + label + " debe tener entre 3 y 50 caracteres");
        }
        if (!RESOURCE_NAME_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("El nombre de " + label + " solo puede contener letras, números, puntos, guiones y guiones bajos");
        }
        return trimmed;
    }

    private String validateExchangeType(String type) {
        String normalized = type != null ? type.trim().toLowerCase() : "";
        if (!EXCHANGE_TYPE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("El tipo de exchange debe ser: direct, topic, fanout o headers");
        }
        return normalized;
    }

    private String validateRoutingKey(String routingKey) {
        if (routingKey == null || routingKey.trim().isEmpty()) {
            throw new IllegalArgumentException("La routing key no puede estar vacía");
        }
        String trimmed = routingKey.trim();
        if (trimmed.length() > 100 || !ROUTING_KEY_PATTERN.matcher(trimmed).matches()) {
            throw new IllegalArgumentException("La routing key contiene caracteres no permitidos o excede 100 caracteres");
        }
        return trimmed;
    }

    @PostMapping("/queues")
    public ResponseEntity<?> createQueue(
            @Valid @RequestBody(required = false) CreateQueueDTO queueDto,
            @RequestParam(required = false) String queueName) {

        if (queueDto != null) {
            resourceManager.createQueue(queueDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Cola '" + queueDto.getName() + "' creada exitosamente",
                            "name", queueDto.getName(),
                            "durable", queueDto.isDurable()
                    ));
        }

        String validName = validateResourceName(queueName, "cola");
        CreateQueueDTO dto = CreateQueueDTO.builder().name(validName).durable(true).build();
        resourceManager.createQueue(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Cola '" + dto.getName() + "' creada exitosamente",
                        "name", dto.getName(),
                        "durable", dto.isDurable()
                ));
    }

    @PostMapping("/exchanges")
    public ResponseEntity<?> createExchange(
            @Valid @RequestBody(required = false) CreateExchangeDTO exchangeDto,
            @RequestParam(required = false) String exchangeName,
            @RequestParam(required = false, defaultValue = "direct") String type) {

        if (exchangeDto != null) {
            resourceManager.createExchange(exchangeDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Exchange '" + exchangeDto.getName() + "' creado exitosamente",
                            "name", exchangeDto.getName(),
                            "type", exchangeDto.getType()
                    ));
        }

        String validName = validateResourceName(exchangeName, "exchange");
        String validType = validateExchangeType(type);
        CreateExchangeDTO dto = CreateExchangeDTO.builder().name(validName).type(validType).durable(true).build();
        resourceManager.createExchange(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Exchange '" + dto.getName() + "' creado exitosamente",
                        "name", dto.getName(),
                        "type", dto.getType()
                ));
    }

    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(
            @Valid @RequestBody(required = false) CreateBindingDTO bindingDto,
            @RequestParam(required = false) String queueName,
            @RequestParam(required = false) String exchangeName,
            @RequestParam(required = false) String routingKey) {

        CreateBindingDTO target = bindingDto;
        if (target == null) {
            target = CreateBindingDTO.builder()
                    .queueName(validateResourceName(queueName, "cola"))
                    .exchangeName(validateResourceName(exchangeName, "exchange"))
                    .routingKey(validateRoutingKey(routingKey))
                    .build();
        }

        resourceManager.createBinding(target);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message", "Binding creado exitosamente",
                        "queue", target.getQueueName(),
                        "exchange", target.getExchangeName(),
                        "routingKey", target.getRoutingKey()
                ));
    }

    @DeleteMapping("/bindings")
    public ResponseEntity<?> deleteBinding(
            @Valid @RequestBody(required = false) CreateBindingDTO bindingDto,
            @RequestParam(required = false) String queueName,
            @RequestParam(required = false) String exchangeName,
            @RequestParam(required = false) String routingKey) {

        CreateBindingDTO target = bindingDto;
        if (target == null) {
            target = CreateBindingDTO.builder()
                    .queueName(validateResourceName(queueName, "cola"))
                    .exchangeName(validateResourceName(exchangeName, "exchange"))
                    .routingKey(validateRoutingKey(routingKey))
                    .build();
        }

        resourceManager.removeBinding(target);
        return ResponseEntity.ok(Map.of(
                "message", "Binding eliminado exitosamente",
                "queue", target.getQueueName(),
                "exchange", target.getExchangeName(),
                "routingKey", target.getRoutingKey()
        ));
    }

    @GetMapping("/queues/{queueName}")
    public ResponseEntity<?> getQueueInfo(@PathVariable String queueName) {
        String validName = validateResourceName(queueName, "cola");
        var info = resourceManager.getQueueInfo(validName);
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/queues/{queueName}")
    public ResponseEntity<Map<String, String>> deleteQueue(@PathVariable String queueName) {
        String validName = validateResourceName(queueName, "cola");
        resourceManager.deleteQueue(validName);
        return ResponseEntity.ok(Map.of("message", "Cola '" + validName + "' eliminada", "name", validName));
    }

    @DeleteMapping("/exchanges/{exchangeName}")
    public ResponseEntity<Map<String, String>> deleteExchange(@PathVariable String exchangeName) {
        String validName = validateResourceName(exchangeName, "exchange");
        resourceManager.deleteExchange(validName);
        return ResponseEntity.ok(Map.of("message", "Exchange '" + validName + "' eliminado", "name", validName));
    }

    @DeleteMapping("/queues/{queueName}/messages")
    public ResponseEntity<Map<String, String>> clearQueueMessages(@PathVariable String queueName) {
        String validName = validateResourceName(queueName, "cola");
        resourceManager.purgeQueue(validName);
        return ResponseEntity.ok(Map.of("message", "Mensajes de la cola '" + validName + "' eliminados", "name", validName));
    }

    @PostMapping("/queues/{queueName}/purge")
    public ResponseEntity<Map<String, String>> purgeQueue(@PathVariable String queueName) {
        return clearQueueMessages(queueName);
    }
}
