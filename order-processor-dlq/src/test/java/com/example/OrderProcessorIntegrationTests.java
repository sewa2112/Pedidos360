package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderProcessorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdmin rabbitAdmin;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("Integracion HTTP: /api/orders/status y /api/orders/send publican en orders.exchange")
    void testOrderEndpointsIntegration() throws Exception {
        mockMvc.perform(get("/api/orders/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backend").value("Online"));

        mockMvc.perform(post("/api/orders/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"ORD-900\",\"customerName\":\"Ana\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Orden enviada"))
                .andExpect(jsonPath("$.orderId").value("ORD-900"));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.ORDERS_EXCHANGE),
                eq(RabbitMQConfig.ORDERS_ROUTING_KEY),
                contains("ORD-900")
        );
    }

    @Test
    @DisplayName("Integracion HTTP: /api/dlq/status y /api/dlq/reprocess operan sobre orders.dlq sin consumidores activos")
    void testDlqEndpointsIntegration() throws Exception {
        when(rabbitAdmin.getQueueInfo(RabbitMQConfig.DLQ_QUEUE))
                .thenReturn(new QueueInformation(RabbitMQConfig.DLQ_QUEUE, 1, 0));

        mockMvc.perform(get("/api/dlq/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queue").value(RabbitMQConfig.DLQ_QUEUE))
                .andExpect(jsonPath("$.messagesInQuarantine").value(1))
                .andExpect(jsonPath("$.consumers").value(0));

        when(rabbitTemplate.receiveAndConvert(RabbitMQConfig.DLQ_QUEUE))
                .thenReturn("Orden recuperada #900");

        mockMvc.perform(post("/api/dlq/reprocess"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("Mensaje recuperado de DLQ y reenviado a la cola principal"));

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                "Orden recuperada #900"
        );
    }
}
