package com.example;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
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

import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitMQAvanzadoIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitAdmin rabbitAdmin;

    @MockBean
    private AdminClient adminClient;

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

    @Test
    @DisplayName("Integracion HTTP: endpoints REST de Kafka administran creacion y listado de topicos")
    void testKafkaEndpointsIntegration() throws Exception {
        // Mock listTopics
        ListTopicsResult listResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> listFuture = mock(KafkaFuture.class);
        when(listFuture.get(anyLong(), any())).thenReturn(Set.of("pedidos-stream", "pedidos-audit-stream"));
        when(listResult.names()).thenReturn(listFuture);
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listResult);

        mockMvc.perform(get("/api/kafka/topics"))
                .andExpect(status().isOk());

        // Mock createTopics
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> createFuture = mock(KafkaFuture.class);
        when(createResult.all()).thenReturn(createFuture);
        when(adminClient.createTopics(any())).thenReturn(createResult);

        mockMvc.perform(post("/api/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"pedidos-stream\",\"partitions\":3,\"replicationFactor\":1,\"retentionMs\":604800000,\"cleanupPolicy\":\"delete\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.topic").value("pedidos-stream"))
                .andExpect(jsonPath("$.partitions").value(3));

        // Mock validation failure
        mockMvc.perform(post("/api/kafka/topics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"partitions\":0}"))
                .andExpect(status().isBadRequest());
    }
}
