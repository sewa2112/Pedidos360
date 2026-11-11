package com.pedidos360.local.consumer;

import com.pedidos360.local.config.RabbitMQConfig;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.service.LocalService;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor de dominio funcional: RESTAURANTE / COCINA
 * Recibe eventos de pedidos creados desde el broker y genera el pedido interno
 * para el restaurante correspondiente utilizando confirmacion manual (ACK).
 */
@Component
public class RestauranteOrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(RestauranteOrderConsumer.class);

    private final LocalService localService;

    public RestauranteOrderConsumer(LocalService localService) {
        this.localService = localService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_RESTAURANT_ORDERS)
    public void recibirPedidoEnCocina(
            Map<String, Object> data,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {

        try {
            if (data == null || data.isEmpty()) {
                throw new IllegalArgumentException("Payload de pedido vacio recibido desde RabbitMQ");
            }
            log.info("[DOMINIO RESTAURANTE] Evento de pedido recibido en cocina: {}", data);

            Integer pedidoId = null;
            if (data.get("pedidoId") != null) {
                pedidoId = Integer.valueOf(data.get("pedidoId").toString());
            }

            String cliente = data.get("cliente") != null ? data.get("cliente").toString() : "Cliente Anonimo";
            String restaurante = data.get("restaurante") != null ? data.get("restaurante").toString() : "Restaurante General";
            String items = data.get("items") != null ? data.get("items").toString() : "Items no especificados";

            Integer montoTotal = 0;
            if (data.get("montoTotal") != null) {
                montoTotal = Integer.valueOf(data.get("montoTotal").toString());
            }

            Pedido pedidoLocal = new Pedido(pedidoId, cliente, restaurante, items, montoTotal, "RECIBIDO_EN_COCINA");
            localService.registrarPedido(pedidoLocal);

            // Confirmacion manual explicita (ACK)
            channel.basicAck(deliveryTag, false);
            log.info("[DOMINIO RESTAURANTE] Pedido #{} confirmado con basicAck hacia RabbitMQ", pedidoLocal.getId());

        } catch (Exception e) {
            log.error("[DOMINIO RESTAURANTE] Error al procesar pedido en cocina: {}", e.getMessage(), e);
            try {
                // Rechazo controlado evitando bucles de reintentos infinitos
                channel.basicNack(deliveryTag, false, false);
            } catch (Exception nackEx) {
                log.error("Fallo al enviar basicNack: {}", nackEx.getMessage());
            }
        }
    }
}
