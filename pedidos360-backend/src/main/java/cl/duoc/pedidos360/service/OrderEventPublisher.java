package cl.duoc.pedidos360.service;

import cl.duoc.pedidos360.config.RabbitMQConfig;
import cl.duoc.pedidos360.models.Pedido;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * Publica de forma desacoplada un evento de creación de pedido hacia RabbitMQ.
     * Si RabbitMQ no está disponible, la excepción se captura para no interrumpir
     * el flujo principal del negocio ni la persistencia en base de datos.
     */
    public void publishOrderCreated(Pedido pedido) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("evento", "PEDIDO_CREADO");
            event.put("pedidoId", pedido.getId());
            event.put("cliente", pedido.getCliente());
            event.put("restaurante", pedido.getRestaurante());
            event.put("montoTotal", pedido.getMontoTotal());
            event.put("estado", pedido.getEstado());
            event.put("fecha", pedido.getFecha() != null ? pedido.getFecha().toString() : null);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_PEDIDOS,
                    RabbitMQConfig.ROUTING_KEY_PEDIDO_CREADO,
                    event
            );

            log.info("Evento de pedido publicado en RabbitMQ [Exchange: {}, RoutingKey: {}, PedidoId: {}]",
                    RabbitMQConfig.EXCHANGE_PEDIDOS, RabbitMQConfig.ROUTING_KEY_PEDIDO_CREADO, pedido.getId());

        } catch (Exception e) {
            log.warn("No se pudo publicar el evento en RabbitMQ (el pedido permanece guardado con éxito en BD): {}", e.getMessage());
        }
    }
}

