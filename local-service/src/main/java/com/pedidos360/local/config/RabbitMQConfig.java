package com.pedidos360.local.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ========== CONSTANTES CENTRALIZADAS DE MENSAJERIA ==========
    public static final String EXCHANGE_ORDERS = "orders.exchange";
    public static final String QUEUE_RESTAURANT_ORDERS = "restaurant.orders.queue";
    public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

    public static final String EXCHANGE_RESTAURANT = "restaurant.exchange";
    public static final String QUEUE_RESTAURANT_STATUS = "restaurant.status.queue";
    public static final String ROUTING_KEY_STATUS_UPDATED = "restaurant.status.updated";

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(EXCHANGE_ORDERS, true, false);
    }

    @Bean
    public Queue restaurantOrdersQueue() {
        return QueueBuilder.durable(QUEUE_RESTAURANT_ORDERS)
                .withArgument("x-message-ttl", 60000)
                .build();
    }

    @Bean
    public Binding restaurantOrdersBinding(Queue restaurantOrdersQueue, DirectExchange ordersExchange) {
        return BindingBuilder.bind(restaurantOrdersQueue)
                .to(ordersExchange)
                .with(ROUTING_KEY_ORDER_CREATED);
    }

    @Bean
    public DirectExchange restaurantExchange() {
        return new DirectExchange(EXCHANGE_RESTAURANT, true, false);
    }

    @Bean
    public Queue restaurantStatusQueue() {
        return QueueBuilder.durable(QUEUE_RESTAURANT_STATUS).build();
    }

    @Bean
    public Binding restaurantStatusBinding(Queue restaurantStatusQueue, DirectExchange restaurantExchange) {
        return BindingBuilder.bind(restaurantStatusQueue)
                .to(restaurantExchange)
                .with(ROUTING_KEY_STATUS_UPDATED);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
