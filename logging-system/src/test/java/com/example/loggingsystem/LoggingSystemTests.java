package com.example.loggingsystem;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class LoggingSystemTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @Autowired
    private LogConsumer logConsumer;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @Test
    @DisplayName("RabbitMQConfig define exchange, colas durables y bindings centralizados")
    void testConfiguracionCentralizadaLogging() {
        assertEquals(RabbitMQConfig.EXCHANGE_NAME, rabbitMQConfig.directExchange().getName());
        assertEquals(RabbitMQConfig.ALL_LOGS_QUEUE, rabbitMQConfig.allLogsQueue().getName());
        assertEquals(RabbitMQConfig.ERRORS_ONLY_QUEUE, rabbitMQConfig.errorsOnlyQueue().getName());
        assertTrue(rabbitMQConfig.allLogsQueue().isDurable());
        assertTrue(rabbitMQConfig.errorsOnlyQueue().isDurable());
    }

    @Test
    @DisplayName("LogConsumer confirma con basicAck logs validos en monitor general y alertas criticas")
    void testLogConsumerManualAck() throws Exception {
        Channel channel = Mockito.mock(Channel.class);

        logConsumer.receiveAllLogs("Inicio de servicio Pedidos360", channel, 11L);
        logConsumer.receiveErrorLogs("Fallo de conexion a RDS", channel, 12L);

        verify(channel).basicAck(11L, false);
        verify(channel).basicAck(12L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    @DisplayName("LogConsumer rechaza con basicNack(requeue=false) mensajes de log vacios")
    void testLogConsumerManualNackOnBlank() throws Exception {
        Channel channel = Mockito.mock(Channel.class);

        logConsumer.receiveAllLogs("   ", channel, 21L);
        logConsumer.receiveErrorLogs(null, channel, 22L);

        verify(channel).basicNack(21L, false, false);
        verify(channel).basicNack(22L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("POST /log envia evento validado hacia logs_direct_exchange y GET /api/logs/status responde Online")
    void testEndpointsLoggingHttp() throws Exception {
        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"error\",\"message\":\"Timeout al procesar pago\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value("ERROR"))
                .andExpect(jsonPath("$.status").value("Log enviado"));

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_NAME),
                eq("ERROR"),
                eq("Timeout al procesar pago")
        );

        mockMvc.perform(get("/api/logs/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("logging-system"))
                .andExpect(jsonPath("$.status").value("Online"));
    }

    @Test
    @DisplayName("POST /log rechaza nivel invalido o mensaje vacio con 400 Bad Request")
    void testValidacionLogMessage() throws Exception {
        mockMvc.perform(post("/log")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"level\":\"TRACE\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validacion fallida"));

        verifyNoInteractions(rabbitTemplate);
    }
}
