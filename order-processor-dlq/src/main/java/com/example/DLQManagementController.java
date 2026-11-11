package com.example;

import com.rabbitmq.client.GetResponse;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dlq")
@CrossOrigin(originPatterns = "*")
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
     * Inspecciona los mensajes almacenados en la DLQ sin eliminarlos:
     * lee mediante basicGet(autoAck=false) y los devuelve inmediatamente a la cola con basicNack(requeue=true).
     */
    @GetMapping("/inspect")
    public ResponseEntity<Map<String, Object>> inspectDLQMessages(
            @RequestParam(defaultValue = "10") int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> inspectedMessages = new ArrayList<>();

        rabbitTemplate.execute(channel -> {
            List<Long> deliveryTags = new ArrayList<>();
            for (int i = 0; i < safeLimit; i++) {
                GetResponse response = channel.basicGet(RabbitMQConfig.DLQ_QUEUE, false);
                if (response == null) {
                    break;
                }
                long tag = response.getEnvelope().getDeliveryTag();
                deliveryTags.add(tag);

                Map<String, Object> item = new LinkedHashMap<>();
                item.put("deliveryTag", tag);
                item.put("redelivered", response.getEnvelope().isRedeliver());
                item.put("routingKey", response.getEnvelope().getRoutingKey());
                item.put("payload", new String(response.getBody(), StandardCharsets.UTF_8));
                inspectedMessages.add(item);
            }

            // Reencola todos los mensajes leidos para preservarlos en cuarentena
            for (Long tag : deliveryTags) {
                channel.basicNack(tag, false, true);
            }
            return null;
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("queue", RabbitMQConfig.DLQ_QUEUE);
        result.put("inspectedCount", inspectedMessages.size());
        result.put("preservedInQueue", true);
        result.put("messages", inspectedMessages);
        return ResponseEntity.ok(result);
    }

    /**
     * Permite recuperar y reprocesar manualmente un mensaje desde la DLQ hacia la cola principal.
     */
    @PostMapping("/reprocess")
    public ResponseEntity<Map<String, String>> reprocessOneMessage() {
        // Obtiene un mensaje de la DLQ confirmandolo manualmente para reenvio
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
