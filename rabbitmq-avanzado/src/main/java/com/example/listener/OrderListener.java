package com.example.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderListener {

    private static final Logger log = LoggerFactory.getLogger(OrderListener.class);

    @RabbitListener(
            id = "order-listener",
            queues = "orders.queue",
            containerFactory = "orderListenerFactory"
    )
    public void processOrder(String orderId) {
        try {
            log.info("Procesando orden: {}", orderId);
            Thread.sleep(2000);
            log.info("Orden procesada: {}", orderId);
        } catch (InterruptedException e) {
            log.error("Error: {}", orderId, e);
            Thread.currentThread().interrupt();
        }
    }
}
