package com.example.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OrderListener {

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

