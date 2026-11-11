package com.example;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitMQAvanzadoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdmin rabbitAdmin;

    @Test
    @DisplayName("Integracion HTTP: validacion rechaza DTOs y tipos de exchange invalidos con 400 Bad Request")
    void testHttpValidationEndpoints() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/queues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"durable\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validación fallida"));

        mockMvc.perform(post("/api/rabbitmq/exchanges")
                        .param("exchangeName", "demo.exchange")
                        .param("type", "invalido"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parámetro inválido"));
    }

    @Test
    @DisplayName("Integracion HTTP: endpoints REST de bindings y limpieza de mensajes funcionan con DELETE")
    void testRestfulBindingsAndPurgeEndpoints() throws Exception {
        mockMvc.perform(post("/api/rabbitmq/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queueName\":\"orders.queue\",\"exchangeName\":\"orders.exchange\",\"routingKey\":\"order.created\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.queue").value("orders.queue"));

        verify(rabbitAdmin).declareBinding(any(Binding.class));

        mockMvc.perform(delete("/api/rabbitmq/bindings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"queueName\":\"orders.queue\",\"exchangeName\":\"orders.exchange\",\"routingKey\":\"order.created\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Binding eliminado exitosamente"));

        verify(rabbitAdmin).removeBinding(any(Binding.class));

        when(rabbitAdmin.purgeQueue("orders.queue")).thenReturn(0);
        mockMvc.perform(delete("/api/rabbitmq/queues/orders.queue/messages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("orders.queue"));

        verify(rabbitAdmin).purgeQueue("orders.queue");
    }

    @Test
    @DisplayName("Integracion HTTP: pausar un listener inexistente devuelve 404 Not Found")
    void testListenerNotFound() throws Exception {
        mockMvc.perform(post("/api/listeners/inexistente/pause"))
                .andExpect(status().isNotFound());
    }
}
