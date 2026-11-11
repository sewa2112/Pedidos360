package com.pedidos360.local.consumer;

import com.pedidos360.local.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor de dominio funcional: RESTAURANTE / SEGUIMIENTO DE ESTADOS
 * Escucha eventos de actualizacion de estado en restaurant.status.queue con confirmacion manual (ACK).
 */
@Component
public class RestauranteStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(RestauranteStatusConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RESTAURANT_STATUS)
    public void recibirActualizacionEstado(
            Map<String, Object> data,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("Evento de estado vacio recibido en restaurant.status.queue");
            }

            Object pedidoId = data.get("pedidoId");
            Object restaurante = data.get("restaurante");
            Object nuevoEstado = data.get("nuevoEstado");

            if (nuevoEstado == null || nuevoEstado.toString().isBlank()) {
                throw new IllegalArgumentException("El campo nuevoEstado es obligatorio");
            }

            log.info("[DOMINIO RESTAURANTE-ESTADO] Pedido #{} ({}) paso a estado '{}'",
                    pedidoId, restaurante, nuevoEstado);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[DOMINIO RESTAURANTE-ESTADO] Error al procesar estado: {}", e.getMessage());
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("Fallo al enviar basicNack en estado de restaurante: {}", nackEx.getMessage());
            }
        }
    }
}
