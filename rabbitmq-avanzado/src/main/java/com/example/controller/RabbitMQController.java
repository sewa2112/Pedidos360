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

@RestController
@RequestMapping({"/api/rabbitmq", ""})
public class RabbitMQController {

    private final RabbitResourceManager resourceManager;

    public RabbitMQController(RabbitResourceManager resourceManager) {
        this.resourceManager = resourceManager;
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

        if (queueName != null && !queueName.trim().isEmpty()) {
            if (!queueName.matches("^[a-zA-Z0-9._-]+$")) {
                throw new IllegalArgumentException("El nombre de la cola solo puede contener letras, números, puntos, guiones y guiones bajos");
            }
            CreateQueueDTO dto = CreateQueueDTO.builder().name(queueName.trim()).durable(true).build();
            resourceManager.createQueue(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Cola '" + dto.getName() + "' creada exitosamente");
        }

        throw new IllegalArgumentException("Debe proporcionar un nombre de cola válido (mediante cuerpo JSON o parámetro 'queueName')");
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

        if (exchangeName != null && !exchangeName.trim().isEmpty()) {
            if (!exchangeName.matches("^[a-zA-Z0-9._-]+$")) {
                throw new IllegalArgumentException("El nombre del exchange solo puede contener letras, números, puntos, guiones y guiones bajos");
            }
            CreateExchangeDTO dto = CreateExchangeDTO.builder().name(exchangeName.trim()).type(type).durable(true).build();
            resourceManager.createExchange(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Exchange '" + dto.getName() + "' creado exitosamente");
        }

        throw new IllegalArgumentException("Debe proporcionar un nombre de exchange válido (mediante cuerpo JSON o parámetro 'exchangeName')");
    }

    @PostMapping("/bindings")
    public ResponseEntity<?> createBinding(
            @Valid @RequestBody(required = false) CreateBindingDTO bindingDto,
            @RequestParam(required = false) String queueName,
            @RequestParam(required = false) String exchangeName,
            @RequestParam(required = false) String routingKey) {

        if (bindingDto != null) {
            resourceManager.createBinding(bindingDto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message", "Binding creado exitosamente",
                            "queue", bindingDto.getQueueName(),
                            "exchange", bindingDto.getExchangeName(),
                            "routingKey", bindingDto.getRoutingKey()
                    ));
        }

        if (queueName != null && exchangeName != null && routingKey != null
                && !queueName.trim().isEmpty() && !exchangeName.trim().isEmpty() && !routingKey.trim().isEmpty()) {
            CreateBindingDTO dto = CreateBindingDTO.builder()
                    .queueName(queueName.trim())
                    .exchangeName(exchangeName.trim())
                    .routingKey(routingKey.trim())
                    .build();
            resourceManager.createBinding(dto);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Binding creado: " + queueName + " <-> " + exchangeName);
        }

        throw new IllegalArgumentException("Parámetros de binding incompletos. Debe indicar queueName, exchangeName y routingKey");
    }

    @GetMapping("/queues/{queueName}")
    public ResponseEntity<?> getQueueInfo(@PathVariable String queueName) {
        var info = resourceManager.getQueueInfo(queueName);
        return info != null ? ResponseEntity.ok(info) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/queues/{queueName}")
    public ResponseEntity<String> deleteQueue(@PathVariable String queueName) {
        resourceManager.deleteQueue(queueName);
        return ResponseEntity.ok("Cola '" + queueName + "' eliminada");
    }

    @DeleteMapping("/exchanges/{exchangeName}")
    public ResponseEntity<String> deleteExchange(@PathVariable String exchangeName) {
        resourceManager.deleteExchange(exchangeName);
        return ResponseEntity.ok("Exchange '" + exchangeName + "' eliminado");
    }

    @PostMapping("/queues/{queueName}/purge")
    public ResponseEntity<String> purgeQueue(@PathVariable String queueName) {
        resourceManager.purgeQueue(queueName);
        return ResponseEntity.ok("Cola '" + queueName + "' purgada");
    }
}
