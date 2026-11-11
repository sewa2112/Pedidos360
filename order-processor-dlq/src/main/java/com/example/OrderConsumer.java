package com.example;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);
    public static final int MAX_RETRIES = 3;

    // Rastreador de reintentos por clave estable del mensaje (messageId o hash del payload)
    private final ConcurrentHashMap<String, AtomicInteger> retryTracker = new ConcurrentHashMap<>();

    /**
     * Consumidor principal: Recibe ordenes de orders.queue.
     * Implementa politica de reintentos finita (max 3) y desvio al DLX tras agotarlos.
     */
    @RabbitListener(queues = RabbitMQConfig.ORDERS_QUEUE)
    public void processOrder(
            String message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag,
            @Header(value = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

        String safeMessage = message != null ? message : "";
        String trackingKey = (messageId != null && !messageId.isBlank())
                ? "id-" + messageId
                : "payload-" + safeMessage.hashCode() + "-" + safeMessage.length();

        AtomicInteger attempts = retryTracker.computeIfAbsent(trackingKey, k -> new AtomicInteger(0));
        int currentAttempt = attempts.incrementAndGet();

        try {
            log.info("[PROCESADOR] Recibida orden (intento {}/{}): {}", currentAttempt, MAX_RETRIES, safeMessage);

            String lower = safeMessage.toLowerCase();
            boolean falloPermanente = lower.contains("error") || lower.contains("fatal") || lower.contains("dlq");
            boolean falloTransitorio = lower.contains("reintento") || lower.contains("transitorio");

            if (falloPermanente) {
                throw new RuntimeException("Fallo permanente al procesar orden: " + safeMessage);
            }

            if (falloTransitorio && currentAttempt < MAX_RETRIES) {
                throw new RuntimeException("Fallo transitorio simulado (recuperable en intento " + MAX_RETRIES + "): " + safeMessage);
            }

            // Procesamiento exitoso
            log.info("[EXITO] Orden procesada correctamente en intento {}: {}", currentAttempt, safeMessage);
            retryTracker.remove(trackingKey);

            // Ack manual hacia RabbitMQ
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.warn("[FALLO] Intento {} fallido para el mensaje: {}. Causa: {}", currentAttempt, safeMessage, e.getMessage());

            try {
                if (currentAttempt < MAX_RETRIES) {
                    // Reintento: Reencola el mensaje en orders.queue
                    log.info("[REINTENTO] Reencolando mensaje (intento {} de {}) con basicNack(requeue=true)", currentAttempt, MAX_RETRIES);
                    channel.basicNack(deliveryTag, false, true);
                } else {
                    // Reintentos agotados: Desvia al Dead Letter Exchange (DLX) con requeue=false
                    log.error("[DLX] Maximo de reintentos alcanzado ({}). Desviando mensaje a DLX con basicNack(requeue=false)", MAX_RETRIES);
                    retryTracker.remove(trackingKey);
                    channel.basicNack(deliveryTag, false, false);
                }
            } catch (Exception nackException) {
                log.error("Fallo critico al emitir basicNack: {}", nackException.getMessage());
            }
        }
    }
}
