package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListenerManagementService {

    private static final Logger log = LoggerFactory.getLogger(ListenerManagementService.class);

    private final RabbitListenerEndpointRegistry registry;

    public ListenerManagementService(RabbitListenerEndpointRegistry registry) {
        this.registry = registry;
    }

    public boolean pauseListener(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            log.warn("Listener no encontrado para pausar: {}", listenerId);
            return false;
        }
        if (container.isRunning()) {
            container.stop();
            log.info("Listener pausado: {}", listenerId);
        }
        return true;
    }

    public boolean resumeListener(String listenerId) {
        MessageListenerContainer container = registry.getListenerContainer(listenerId);
        if (container == null) {
            log.warn("Listener no encontrado para reanudar: {}", listenerId);
            return false;
        }
        container.start();
        log.info("Listener reanudado: {}", listenerId);
        return true;
    }

    public List<String> getAllListenerIds() {
        return registry.getListenerContainerIds().stream().toList();
    }
}
