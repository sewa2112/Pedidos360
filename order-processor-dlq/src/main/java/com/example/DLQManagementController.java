package com.example;

import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
@CrossOrigin(origins = "*")
public class DLQManagementController {

    private final RabbitAdmin rabbitAdmin;
    private final RabbitTemplate rabbitTemplate;

    public DLQManagementController(RabbitAdmin rabbitAdmin, RabbitTemplate rabbitTemplate) {
        this.rabbitAdmin = rabbitAdmin;
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Consulta el estado de la cola de cuarentena (DLQ) sin retirar los mensajes.
     * Permite inspeccion manual y auditoria sin consumo destructivo.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDLQStatus() {
        QueueInformation info = rabbitAdmin.getQueueInfo(RabbitMQConfig.DLQ_QUEUE);
        Map<String, Object> response = new HashMap<>();
        response.put("queue", RabbitMQConfig.DLQ_QUEUE);
        response.put("messagesInQuarantine", info != null ? info.getMessageCount() : 0);
        response.put("consumers", info != null ? info.getConsumerCount() : 0);
        response.put("policy", "Retencion por 24 horas (TTL: 86400000ms)");
        return ResponseEntity.ok(response);
    }

    /**
     * Permite recuperar y reprocesar manualmente un mensaje desde la DLQ hacia la cola principal.
     */
    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, String>> reprocessOneMessage() {
        // Obtiene un mensaje de la DLQ confirmandolo
        Object message = rabbitTemplate.receiveAndConvert(RabbitMQConfig.DLQ_QUEUE);
        Map<String, String> result = new HashMap<>();

        if (message != null) {
            // Reinyecta a la cola principal a traves del exchange
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDERS_EXCHANGE,
                    RabbitMQConfig.ORDERS_ROUTING_KEY,
                    message
            );
            result.put("status", "Mensaje recuperado de DLQ y reenviado a la cola principal");
            result.put("messagePayload", message.toString());
            return ResponseEntity.ok(result);
        } else {
            result.put("status", "No hay mensajes en la DLQ para reprocesar");
            return ResponseEntity.ok(result);
        }
    }
}
