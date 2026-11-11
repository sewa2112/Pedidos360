package com.example;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.rabbit.core.ChannelCallback;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderProcessorDlqTests {

    @Mock
    private Channel channel;

    @Mock
    private RabbitAdmin rabbitAdmin;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("RabbitMQConfig define colas, DLX y retencion de 24h en DLQ de forma centralizada")
    void testConfiguracionCentralizadaDLQ() {
        RabbitMQConfig config = new RabbitMQConfig();
        Queue ordersQueue = config.ordersQueue();
        Queue dlqQueue = config.deadLetterQueue();
        FanoutExchange dlx = config.deadLetterExchange();

        assertEquals(RabbitMQConfig.ORDERS_QUEUE, ordersQueue.getName());
        assertEquals(RabbitMQConfig.DLX_EXCHANGE, ordersQueue.getArguments().get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.DLQ_QUEUE, dlqQueue.getName());
        assertEquals(86400000, dlqQueue.getArguments().get("x-message-ttl"));
        assertEquals(RabbitMQConfig.DLX_EXCHANGE, dlx.getName());
    }

    @Test
    @DisplayName("OrderConsumer confirma con basicAck un pedido normal en el primer intento")
    void testProcesamientoExitosoPrimerIntento() throws Exception {
        OrderConsumer consumer = new OrderConsumer();
        consumer.processOrder("Orden normal #101", channel, 1L, "msg-101");

        verify(channel, times(1)).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("OrderConsumer reintenta fallos transitorios aunque cambie el deliveryTag y confirma al 3er intento")
    void testReintentoTransitorioRecuperado() throws Exception {
        OrderConsumer consumer = new OrderConsumer();
        String payload = "Orden con reintento transitorio #202";

        consumer.processOrder(payload, channel, 1L, null);
        consumer.processOrder(payload, channel, 2L, null);
        consumer.processOrder(payload, channel, 3L, null);

        verify(channel).basicNack(1L, false, true);
        verify(channel).basicNack(2L, false, true);
        verify(channel).basicAck(3L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), eq(false));
    }

    @Test
    @DisplayName("OrderConsumer deriva a DLX con basicNack(requeue=false) tras agotar 3 intentos en fallo permanente")
    void testFalloPermanenteDerivaADLQTras3Reintentos() throws Exception {
        OrderConsumer consumer = new OrderConsumer();
        String payload = "Orden con error critico #303";

        consumer.processOrder(payload, channel, 10L, null);
        consumer.processOrder(payload, channel, 11L, null);
        consumer.processOrder(payload, channel, 12L, null);

        verify(channel).basicNack(10L, false, true);
        verify(channel).basicNack(11L, false, true);
        verify(channel).basicNack(12L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("DLQManagementController consulta cuarentena sin borrar mensajes y permite reprocesamiento manual")
    void testDLQControllerStatusYReprocesamiento() {
        DLQManagementController controller = new DLQManagementController(rabbitAdmin, rabbitTemplate);

        QueueInformation mockInfo = new QueueInformation(RabbitMQConfig.DLQ_QUEUE, 2, 0);
        when(rabbitAdmin.getQueueInfo(RabbitMQConfig.DLQ_QUEUE)).thenReturn(mockInfo);

        ResponseEntity<Map<String, Object>> statusResp = controller.getDLQStatus();
        assertEquals(200, statusResp.getStatusCode().value());
        assertEquals(2, statusResp.getBody().get("messagesInQuarantine"));
        assertEquals(0, statusResp.getBody().get("consumers"));

        when(rabbitTemplate.receiveAndConvert(RabbitMQConfig.DLQ_QUEUE)).thenReturn("Orden recuperada #404");
        ResponseEntity<Map<String, String>> reprocessResp = controller.reprocessOneMessage();
        assertEquals(200, reprocessResp.getStatusCode().value());
        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDERS_EXCHANGE,
                RabbitMQConfig.ORDERS_ROUTING_KEY,
                "Orden recuperada #404"
        );
    }

    @Test
    @DisplayName("DLQManagementController inspecciona mensajes conservandolos intactos en DLQ mediante basicNack(requeue=true)")
    void testDLQInspectNonDestructive() throws Exception {
        DLQManagementController controller = new DLQManagementController(rabbitAdmin, rabbitTemplate);

        GetResponse msg1 = new GetResponse(
                new Envelope(101L, false, RabbitMQConfig.DLX_EXCHANGE, RabbitMQConfig.DLX_ROUTING_KEY),
                null,
                "Orden error #1".getBytes(StandardCharsets.UTF_8),
                1
        );
        GetResponse msg2 = new GetResponse(
                new Envelope(102L, true, RabbitMQConfig.DLX_EXCHANGE, RabbitMQConfig.DLX_ROUTING_KEY),
                null,
                "Orden error #2".getBytes(StandardCharsets.UTF_8),
                0
        );

        when(channel.basicGet(RabbitMQConfig.DLQ_QUEUE, false))
                .thenReturn(msg1, msg2, null);

        when(rabbitTemplate.execute(any(ChannelCallback.class))).thenAnswer(invocation -> {
            ChannelCallback<?> callback = invocation.getArgument(0);
            return callback.doInRabbit(channel);
        });

        ResponseEntity<Map<String, Object>> resp = controller.inspectDLQMessages(10);
        assertEquals(200, resp.getStatusCode().value());
        assertEquals(2, resp.getBody().get("inspectedCount"));
        assertEquals(true, resp.getBody().get("preservedInQueue"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> messages = (List<Map<String, Object>>) resp.getBody().get("messages");
        assertEquals("Orden error #1", messages.get(0).get("payload"));
        assertEquals("Orden error #2", messages.get(1).get("payload"));

        // Verifica que no se consumieron de forma destructiva
        verify(channel).basicNack(101L, false, true);
        verify(channel).basicNack(102L, false, true);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
