package cl.duoc.pedidos360.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ========== CONSTANTES DE CONFIGURACIÓN CENTRALIZADA ==========
    public static final String EXCHANGE_PEDIDOS = "orders.exchange";
    public static final String ROUTING_KEY_PEDIDO_CREADO = "order.created";
    public static final String QUEUE_PEDIDOS = "orders.queue";

    @Bean
    public DirectExchange ordersExchange() {
        return new DirectExchange(EXCHANGE_PEDIDOS, true, false);
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

