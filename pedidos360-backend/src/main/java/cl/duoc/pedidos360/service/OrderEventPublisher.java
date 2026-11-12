package cl.duoc.pedidos360.service;

import cl.duoc.pedidos360.config.KafkaProducerConfig;
import cl.duoc.pedidos360.config.RabbitMQConfig;
import cl.duoc.pedidos360.dto.PedidoAuditDTO;
import cl.duoc.pedidos360.dto.PedidoStreamEventDTO;
import cl.duoc.pedidos360.models.Pedido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisher(
            RabbitTemplate rabbitTemplate,
            @Autowired(required = false) KafkaTemplate<String, Object> kafkaTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Publica el evento hacia la arquitectura hibrida:
     * 1. RabbitMQ: Para cola de trabajo transaccional (cocina/DLQ).
     * 2. Apache Kafka: Para streaming continuo de eventos (topicos pedidos-stream y pedidos-audit-stream).
     */
    public void publishOrderCreated(Pedido pedido) {
        publishToRabbitMQ(pedido);
        publishToKafka(pedido);
    }

    private void publishToRabbitMQ(Pedido pedido) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("evento", "PEDIDO_CREADO");
            event.put("pedidoId", pedido.getId());
            event.put("cliente", pedido.getCliente());
            event.put("restaurante", pedido.getRestaurante());
            event.put("items", pedido.getItems());
            event.put("direccion", pedido.getDireccion());
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
            log.warn("No se pudo publicar el evento en RabbitMQ: {}", e.getMessage());
        }
    }

    private void publishToKafka(Pedido pedido) {
        if (kafkaTemplate == null) {
            log.debug("KafkaTemplate no configurado, omitiendo emision de stream");
            return;
        }

        String messageKey = pedido.getId() != null ? pedido.getId().toString() : "0";

        // 1. Publicacion en topico principal: pedidos-stream
        try {
            PedidoStreamEventDTO streamDto = new PedidoStreamEventDTO(
                    pedido.getId(),
                    pedido.getCliente(),
                    pedido.getRestaurante(),
                    pedido.getItems(),
                    pedido.getDireccion(),
                    pedido.getMontoTotal(),
                    pedido.getEstado(),
                    pedido.getFecha() != null ? pedido.getFecha().toString() : null
            );

            kafkaTemplate.send(KafkaProducerConfig.TOPIC_PEDIDOS_STREAM, messageKey, streamDto);
            log.info("Evento publicado en Kafka [Topic: {}, Key: {}, PedidoId: {}]",
                    KafkaProducerConfig.TOPIC_PEDIDOS_STREAM, messageKey, pedido.getId());
        } catch (Exception e) {
            log.warn("No se pudo publicar en el topico pedidos-stream de Kafka: {}", e.getMessage());
        }

        // 2. Publicacion en topico de auditoria: pedidos-audit-stream
        try {
            PedidoAuditDTO auditDto = new PedidoAuditDTO(
                    "PEDIDO_CREADO",
                    "PEDIDO",
                    messageKey,
                    pedido.getCliente(),
                    "Pedido registrado para restaurante: " + pedido.getRestaurante()
            );

            kafkaTemplate.send(KafkaProducerConfig.TOPIC_PEDIDOS_AUDIT, messageKey, auditDto);
            log.info("Evento de auditoria publicado en Kafka [Topic: {}, Key: {}]",
                    KafkaProducerConfig.TOPIC_PEDIDOS_AUDIT, messageKey);
        } catch (Exception e) {
            log.warn("No se pudo publicar en el topico pedidos-audit-stream de Kafka: {}", e.getMessage());
        }
    }
}
