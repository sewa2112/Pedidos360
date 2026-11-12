package com.pedidos360.local.kafka;

import com.pedidos360.local.config.KafkaConsumerConfig;
import com.pedidos360.local.dto.PedidoStreamEventDTO;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.service.LocalService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor de dominio funcional: COCINA / PREPARACION (Kafka Stream).
 * Consumer Group: pedidos360-cocina-group
 */
@Component
public class CocinaStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(CocinaStreamConsumer.class);

    private final LocalService localService;

    public CocinaStreamConsumer(LocalService localService) {
        this.localService = localService;
    }

    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_PEDIDOS_STREAM,
            groupId = KafkaConsumerConfig.GROUP_COCINA,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void procesarOrdenEnCocina(@Payload(required = false) PedidoStreamEventDTO evento) {
        try {
            if (evento == null || evento.getPedidoId() == null) {
                log.warn("[KAFKA-COCINA] Evento descartado por payload nulo o sin pedidoId");
                return;
            }

            log.info("[KAFKA-COCINA] Evento recibido en grupo {}: Pedido #{} para {}",
                    KafkaConsumerConfig.GROUP_COCINA, evento.getPedidoId(), evento.getRestaurante());

            Pedido pedidoLocal = new Pedido(
                    evento.getPedidoId(),
                    evento.getCliente() != null ? evento.getCliente() : "Cliente Streaming",
                    evento.getRestaurante() != null ? evento.getRestaurante() : "Restaurante General",
                    evento.getItems() != null ? evento.getItems() : "Sin items especificados",
                    evento.getMontoTotal() != null ? evento.getMontoTotal() : 0,
                    "STREAM_EN_COCINA"
            );

            localService.registrarPedido(pedidoLocal);
            log.info("[KAFKA-COCINA] Pedido #{} registrado exitosamente desde Kafka Stream", evento.getPedidoId());

        } catch (Exception e) {
            log.error("[KAFKA-COCINA] Error procesando evento de cocina: {}", e.getMessage(), e);
            throw e; // Permite que el ErrorHandler aplique reintentos controlados
        }
    }
}
