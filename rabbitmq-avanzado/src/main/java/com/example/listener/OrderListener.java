package com.example.listener;

import com.example.config.RabbitAdminConfig;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    @RabbitListener(
            id = RabbitAdminConfig.LISTENER_ORDER_ID,
            queues = RabbitAdminConfig.ADMIN_MONITOR_QUEUE,
            containerFactory = "orderListenerFactory"
    )
    public void processOrder(
            String payload,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("[MONITOR-ADMIN] Evento recibido en cola de auditoria {}: {}",
                    RabbitAdminConfig.ADMIN_MONITOR_QUEUE, payload);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("[MONITOR-ADMIN] Error al procesar evento de auditoria: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException nackEx) {
                log.error("[MONITOR-ADMIN] Error critico al emitir basicNack: {}", nackEx.getMessage());
            }
        }
    }
}
