package com.example;

import com.example.config.RabbitAdminConfig;
import com.example.controller.KafkaTopicAdminController;
import com.example.controller.RabbitMQController;
import com.example.dto.CreateBindingDTO;
import com.example.dto.CreateExchangeDTO;
import com.example.dto.CreateKafkaTopicDTO;
import com.example.dto.CreateQueueDTO;
import com.example.listener.OrderListener;
import com.example.service.KafkaTopicAdminService;
import com.example.service.RabbitResourceManager;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQAvanzadoTests {

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private Channel channel;

    @Mock
    private AdminClient adminClient;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("RabbitAdminConfig centraliza nombres de cola de monitoreo y bean Queue")
    void testConfiguracionCentralizadaAdmin() {
        RabbitAdminConfig config = new RabbitAdminConfig();
        Queue queue = config.adminMonitorQueue();

        assertEquals(RabbitAdminConfig.ADMIN_MONITOR_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    @DisplayName("RabbitResourceManager encapsula creacion y eliminacion de colas, exchanges y bindings via RabbitAdmin")
    void testRabbitResourceManagerEncapsulacion() {
        RabbitResourceManager manager = new RabbitResourceManager(rabbitAdmin);

        CreateQueueDTO queueDTO = CreateQueueDTO.builder().name("test.queue").durable(true).build();
        manager.createQueue(queueDTO);
        verify(rabbitAdmin, times(1)).declareQueue(any(Queue.class));

        CreateExchangeDTO exchangeDTO = CreateExchangeDTO.builder().name("test.exchange").type("direct").durable(true).build();
        manager.createExchange(exchangeDTO);
        verify(rabbitAdmin, times(1)).declareExchange(any(Exchange.class));

        CreateBindingDTO bindingDTO = CreateBindingDTO.builder()
                .queueName("test.queue")
                .exchangeName("test.exchange")
                .routingKey("order.created")
                .build();
        manager.createBinding(bindingDTO);
        verify(rabbitAdmin, times(1)).declareBinding(any(Binding.class));

        manager.removeBinding(bindingDTO);
        verify(rabbitAdmin, times(1)).removeBinding(any(Binding.class));
    }

    @Test
    @DisplayName("RabbitMQController valida parametros principales y secundarios y delega creacion")
    void testControllerValidacionYCreacion() {
        RabbitResourceManager manager = new RabbitResourceManager(rabbitAdmin);
        RabbitMQController controller = new RabbitMQController(manager);

        CreateQueueDTO validDto = CreateQueueDTO.builder().name("pedidos.valid.queue").durable(true).build();
        ResponseEntity<?> resp = controller.createQueue(validDto, null);
        assertEquals(HttpStatus.CREATED, resp.getStatusCode());

        assertThrows(IllegalArgumentException.class, () -> controller.createQueue(null, "invalid queue!"));
        assertThrows(IllegalArgumentException.class, () -> controller.createQueue(null, "   "));
        assertThrows(IllegalArgumentException.class, () -> controller.createExchange(null, "valid.ex", "invalid_type"));
        assertThrows(IllegalArgumentException.class, () -> controller.getQueueInfo("ab"));
    }

    @Test
    @DisplayName("Bean Validation rechaza DTOs con campos vacios, tamanos invalidos o formato incorrecto")
    void testDtoValidationCompleta() {
        CreateQueueDTO invalidEmpty = CreateQueueDTO.builder().name("").durable(true).build();
        assertFalse(validator.validate(invalidEmpty).isEmpty());

        CreateQueueDTO invalidChars = CreateQueueDTO.builder().name("cola con espacios$").durable(true).build();
        assertFalse(validator.validate(invalidChars).isEmpty());

        CreateExchangeDTO blankType = CreateExchangeDTO.builder().name("valid.exchange").type("").durable(true).build();
        Set<ConstraintViolation<CreateExchangeDTO>> exchangeViolations = validator.validate(blankType);
        assertFalse(exchangeViolations.isEmpty());

        CreateBindingDTO invalidBinding = CreateBindingDTO.builder()
                .queueName("ab")
                .exchangeName("valid.exchange")
                .routingKey("bad routing key!")
                .build();
        Set<ConstraintViolation<CreateBindingDTO>> bindingViolations = validator.validate(invalidBinding);
        assertFalse(bindingViolations.isEmpty());
    }

    @Test
    @DisplayName("OrderListener confirma eventos con ACK manual")
    void testOrderListenerManualAck() throws Exception {
        OrderListener listener = new OrderListener();
        listener.processOrder("Evento de auditoria #501", channel, 7L);

        verify(channel, times(1)).basicAck(7L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("CreateKafkaTopicDTO valida nombres no vacios, particiones y factor de replicacion positivos")
    void testKafkaTopicDtoValidation() {
        CreateKafkaTopicDTO validDto = new CreateKafkaTopicDTO("pedidos-stream", 3, (short) 1, 604800000L, "delete");
        assertTrue(validator.validate(validDto).isEmpty());
        assertEquals("604800000", validDto.toTopicConfigs().get("retention.ms"));
        assertEquals("delete", validDto.toTopicConfigs().get("cleanup.policy"));

        CreateKafkaTopicDTO invalidName = new CreateKafkaTopicDTO("", 3, (short) 1, 604800000L, "delete");
        assertFalse(validator.validate(invalidName).isEmpty());

        CreateKafkaTopicDTO invalidPartitions = new CreateKafkaTopicDTO("pedidos-stream", 0, (short) 1, 604800000L, "delete");
        assertFalse(validator.validate(invalidPartitions).isEmpty());

        CreateKafkaTopicDTO invalidReplication = new CreateKafkaTopicDTO("pedidos-stream", 1, (short) 0, 604800000L, "delete");
        assertFalse(validator.validate(invalidReplication).isEmpty());
    }

    @Test
    @DisplayName("KafkaTopicAdminService delega creacion, listado y eliminacion hacia AdminClient de Kafka")
    void testKafkaTopicAdminServiceDelegation() throws Exception {
        KafkaTopicAdminService service = new KafkaTopicAdminService(adminClient);

        // 1. Test createTopic
        CreateTopicsResult createResult = mock(CreateTopicsResult.class);
        KafkaFuture<Void> createFuture = mock(KafkaFuture.class);
        when(createResult.all()).thenReturn(createFuture);
        when(adminClient.createTopics(any(java.util.Collection.class))).thenReturn(createResult);

        CreateKafkaTopicDTO dto = new CreateKafkaTopicDTO("pedidos-stream", 3, (short) 1, 604800000L, "delete");
        assertTrue(service.createTopic(dto));
        verify(adminClient).createTopics(any(java.util.Collection.class));

        // 2. Test listTopics
        ListTopicsResult listResult = mock(ListTopicsResult.class);
        KafkaFuture<Set<String>> listFuture = mock(KafkaFuture.class);
        when(listFuture.get(anyLong(), any())).thenReturn(Set.of("pedidos-stream", "pedidos-audit-stream"));
        when(listResult.names()).thenReturn(listFuture);
        when(adminClient.listTopics(any(ListTopicsOptions.class))).thenReturn(listResult);

        var topics = service.listTopics();
        assertEquals(2, topics.size());
        assertTrue(topics.contains("pedidos-stream"));

        // 3. Test deleteTopic
        DeleteTopicsResult deleteResult = mock(DeleteTopicsResult.class);
        KafkaFuture<Void> deleteFuture = mock(KafkaFuture.class);
        when(deleteResult.all()).thenReturn(deleteFuture);
        when(adminClient.deleteTopics(any(java.util.Collection.class))).thenReturn(deleteResult);

        assertTrue(service.deleteTopic("pedidos-stream"));
        verify(adminClient).deleteTopics(any(java.util.Collection.class));
    }
}
