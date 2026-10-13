package com.example.service;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import com.example.dto.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RabbitResourceManager {

    private final RabbitAdmin rabbitAdmin;

    public void createQueue(String queueName) {
        createQueue(CreateQueueDTO.builder().name(queueName).durable(true).build());
    }

    public void createQueue(com.example.dto.CreateQueueDTO dto) {
        try {
            Queue queue = dto.isDurable() 
                    ? QueueBuilder.durable(dto.getName()).build() 
                    : QueueBuilder.nonDurable(dto.getName()).build();
            rabbitAdmin.declareQueue(queue);
            log.info("Cola creada exitosamente: {} (durable={})", dto.getName(), dto.isDurable());
        } catch (Exception e) {
            log.error("Error al crear cola {}: {}", dto.getName(), e.getMessage());
            throw new RuntimeException("Error al crear cola en RabbitMQ: " + e.getMessage(), e);
        }
    }

    public void createExchange(String exchangeName) {
        createExchange(com.example.dto.CreateExchangeDTO.builder().name(exchangeName).type("direct").durable(true).build());
    }

    public void createExchange(com.example.dto.CreateExchangeDTO dto) {
        try {
            Exchange exchange;
            String type = dto.getType().toLowerCase();
            switch (type) {
                case "topic" -> exchange = new TopicExchange(dto.getName(), dto.isDurable(), false);
                case "fanout" -> exchange = new FanoutExchange(dto.getName(), dto.isDurable(), false);
                case "headers" -> exchange = new HeadersExchange(dto.getName(), dto.isDurable(), false);
                default -> exchange = new DirectExchange(dto.getName(), dto.isDurable(), false);
            }
            rabbitAdmin.declareExchange(exchange);
            log.info("Exchange creado exitosamente: {} [tipo: {}, durable: {}]", dto.getName(), type, dto.isDurable());
        } catch (Exception e) {
            log.error("Error al crear exchange {}: {}", dto.getName(), e.getMessage());
            throw new RuntimeException("Error al crear exchange en RabbitMQ: " + e.getMessage(), e);
        }
    }

    public void createBinding(String queueName, String exchangeName, String routingKey) {
        createBinding(com.example.dto.CreateBindingDTO.builder().queueName(queueName).exchangeName(exchangeName).routingKey(routingKey).build());
    }

    public void createBinding(com.example.dto.CreateBindingDTO dto) {
        try {
            Binding binding = BindingBuilder
                    .bind(new Queue(dto.getQueueName()))
                    .to(new DirectExchange(dto.getExchangeName()))
                    .with(dto.getRoutingKey());
            rabbitAdmin.declareBinding(binding);
            log.info("Binding creado: {} -> {} [key: {}]", dto.getQueueName(), dto.getExchangeName(), dto.getRoutingKey());
        } catch (Exception e) {
            log.error("Error al crear binding: {}", e.getMessage());
            throw new RuntimeException("Error al crear binding en RabbitMQ: " + e.getMessage(), e);
        }
    }

    public QueueInformation getQueueInfo(String queueName) {
        try {
            QueueInformation info = rabbitAdmin.getQueueInfo(queueName);
            if (info != null) {
                log.info("Cola: {}, Mensajes: {}, Consumidores: {}",
                        info.getName(), info.getMessageCount(), info.getConsumerCount());
                return info;
            }
            return null;
        } catch (Exception e) {
            log.error("Error al obtener info de cola {}: {}", queueName, e.getMessage());
            return null;
        }
    }

    public void deleteQueue(String queueName) {
        try {
            rabbitAdmin.deleteQueue(queueName);
            log.info("Cola eliminada: {}", queueName);
        } catch (Exception e) {
            log.error("Error al eliminar cola {}: {}", queueName, e.getMessage());
        }
    }

    public void deleteExchange(String exchangeName) {
        try {
            rabbitAdmin.deleteExchange(exchangeName);
            log.info("Exchange eliminado: {}", exchangeName);
        } catch (Exception e) {
            log.error("Error al eliminar exchange {}: {}", exchangeName, e.getMessage());
        }
    }

    public void purgeQueue(String queueName) {
        try {
            int purged = rabbitAdmin.purgeQueue(queueName);
            log.info("Cola purgada: {}, mensajes eliminados: {}", queueName, purged);
        } catch (Exception e) {
            log.error("Error al purgar cola {}: {}", queueName, e.getMessage());
        }
    }
}

