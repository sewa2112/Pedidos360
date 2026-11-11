package com.example.loggingsystem;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Consumidor de dominio funcional: AUDITORIA Y ALERTAS DE LOGS.
 * Implementa confirmacion manual (ACK/NACK) y manejo explicito de errores.
 */
@Service
public class LogConsumer {

    private static final Logger log = LoggerFactory.getLogger(LogConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.ALL_LOGS_QUEUE)
    public void receiveAllLogs(
            String message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processLogEvent("MONITOR GENERAL", message, channel, deliveryTag);
    }

    @RabbitListener(queues = RabbitMQConfig.ERRORS_ONLY_QUEUE)
    public void receiveErrorLogs(
            String message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        processLogEvent("ALERTA CRITICA", message, channel, deliveryTag);
    }

    private void processLogEvent(String category, String message, Channel channel, long deliveryTag) {
        try {
            if (message == null || message.isBlank()) {
                throw new IllegalArgumentException("Evento de log vacio recibido en canal " + category);
            }

            if ("ALERTA CRITICA".equals(category)) {
                log.error("[{}] Error critico detectado: {}", category, message);
            } else {
                log.info("[{}] Log recibido: {}", category, message);
            }

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.warn("[{}] Error procesando log, ejecutando basicNack(requeue=false): {}", category, e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackEx) {
                log.error("[{}] Fallo critico al emitir basicNack: {}", category, nackEx.getMessage());
            }
        }
    }
}
