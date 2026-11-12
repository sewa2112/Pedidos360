package com.pedidos360.local.kafka;

import com.pedidos360.local.config.KafkaConsumerConfig;
import com.pedidos360.local.dto.PedidoAuditDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidor de dominio funcional: AUDITORIA Y TRAZABILIDAD (Kafka Stream).
 * Consumer Group: pedidos360-auditoria-group
 * Demuestra como un segundo consumer group lee el mismo flujo o flujo de auditoria
 * de forma independiente sin interferir con el grupo de cocina.
 */
@Component
public class AuditoriaStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaStreamConsumer.class);

    @KafkaListener(
            topics = KafkaConsumerConfig.TOPIC_PEDIDOS_AUDIT,
            groupId = KafkaConsumerConfig.GROUP_AUDITORIA,
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void procesarAuditoria(@Payload(required = false) PedidoAuditDTO audit) {
        if (audit == null) {
            log.warn("[KAFKA-AUDIT] Evento de auditoria descartado por payload vacio");
            return;
        }

        log.info("[KAFKA-AUDIT] Grupo {}: Evento [{}] Entidad [{}:{}] Usuario [{}] Detalle: {}",
                KafkaConsumerConfig.GROUP_AUDITORIA,
                audit.getTipoEvento(),
                audit.getEntidad(),
                audit.getIdEntidad(),
                audit.getUsuario(),
                audit.getDetalle());
    }
}
