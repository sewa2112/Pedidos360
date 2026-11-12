package com.pedidos360.local;

import com.pedidos360.local.consumer.RestauranteOrderConsumer;
import com.pedidos360.local.consumer.RestauranteStatusConsumer;
import com.pedidos360.local.controller.LocalController;
import com.pedidos360.local.dto.CreateLocalDTO;
import com.pedidos360.local.dto.UpdateEstadoPedidoDTO;
import com.pedidos360.local.models.Local;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.service.LocalService;
import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class LocalServiceApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private LocalService localService;

    @Autowired
    private LocalController localController;

    @Autowired
    private RestauranteOrderConsumer restauranteOrderConsumer;

    @Autowired
    private RestauranteStatusConsumer restauranteStatusConsumer;

    @Autowired
    private com.pedidos360.local.kafka.CocinaStreamConsumer cocinaStreamConsumer;

    @Autowired
    private com.pedidos360.local.kafka.AuditoriaStreamConsumer auditoriaStreamConsumer;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    @DisplayName("El contexto de Spring Boot de local-service carga correctamente")
    void contextLoads() {
        assertNotNull(localService);
        assertNotNull(localController);
        assertNotNull(restauranteOrderConsumer);
        assertNotNull(restauranteStatusConsumer);
        assertNotNull(cocinaStreamConsumer);
        assertNotNull(auditoriaStreamConsumer);
    }

    @Test
    @DisplayName("LocalService lista los locales precargados de prueba")
    void testListarLocales() {
        List<Local> locales = localService.listarLocales();
        assertNotNull(locales);
        assertFalse(locales.isEmpty(), "Deben existir locales iniciales sembrados");
    }

    @Test
    @DisplayName("LocalController permite registrar un nuevo local mediante DTO")
    void testCrearLocalExitoso() {
        CreateLocalDTO dto = new CreateLocalDTO("Chifa Express", "Gran Avenida 5500", "+56988887777", "Peruana", true);
        ResponseEntity<Local> response = localController.crearLocal(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Chifa Express", response.getBody().getNombre());
    }

    @Test
    @DisplayName("Registro de pedido en cocina del local y actualizacion de estado funcional")
    void testFlujoPedidoCocina() {
        Pedido pedido = new Pedido(205, "Maria Lopez", "Sushi Roll", "2x Roll Tempura", 18900, "RECIBIDO");
        Pedido guardado = localService.registrarPedido(pedido);

        assertNotNull(guardado.getId());
        assertEquals("RECIBIDO", guardado.getEstado());

        UpdateEstadoPedidoDTO updateDTO = new UpdateEstadoPedidoDTO("EN_PREPARACION");
        ResponseEntity<Pedido> responseUpdate = localController.actualizarEstado(guardado.getId(), updateDTO);

        assertEquals(HttpStatus.OK, responseUpdate.getStatusCode());
        assertNotNull(responseUpdate.getBody());
        assertEquals("EN_PREPARACION", responseUpdate.getBody().getEstado());
    }

    @Test
    @DisplayName("RestauranteOrderConsumer registra pedido recibido y confirma con basicAck manual")
    void testRestauranteOrderConsumerManualAck() throws Exception {
        Channel channel = Mockito.mock(Channel.class);

        Map<String, Object> evento = new HashMap<>();
        evento.put("pedidoId", 310);
        evento.put("cliente", "Carlos Perez");
        evento.put("restaurante", "Pizzeria Napoli");
        evento.put("items", "1x Pizza Pepperoni");
        evento.put("montoTotal", 14500);

        restauranteOrderConsumer.recibirPedidoEnCocina(evento, channel, 42L);

        verify(channel).basicAck(42L, false);
        List<Pedido> pedidos = localService.listarPedidosPorRestaurante("Pizzeria Napoli");
        assertTrue(pedidos.stream().anyMatch(p -> Integer.valueOf(310).equals(p.getPedidoId()) && "1x Pizza Pepperoni".equals(p.getItems())));
    }

    @Test
    @DisplayName("Consumidores de dominio rechazan payloads vacios con basicNack(requeue=false) y confirman estados validos")
    void testRestauranteConsumersAckYNack() throws Exception {
        Channel channel = Mockito.mock(Channel.class);

        // Evento invalido de pedido en cocina -> basicNack requeue=false
        restauranteOrderConsumer.recibirPedidoEnCocina(Map.of(), channel, 50L);
        verify(channel).basicNack(50L, false, false);

        // Evento valido de actualizacion de estado -> basicAck
        restauranteStatusConsumer.recibirActualizacionEstado(
                Map.of("pedidoId", 310, "restaurante", "Pizzeria Napoli", "nuevoEstado", "LISTO_PARA_RETIRO"),
                channel,
                51L
        );
        verify(channel).basicAck(51L, false);
        verify(channel, never()).basicNack(51L, false, false);

        // Evento invalido de actualizacion de estado -> basicNack requeue=false
        restauranteStatusConsumer.recibirActualizacionEstado(Map.of("pedidoId", 310), channel, 52L);
        verify(channel).basicNack(52L, false, false);
    }

    @Test
    @DisplayName("Integracion HTTP: /api/locales valida DTOs, lista locales y actualiza estado en cocina")
    void testEndpointsHttpLocalService() throws Exception {
        mockMvc.perform(get("/api/locales"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").isNotEmpty());

        mockMvc.perform(post("/api/locales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\",\"direccion\":\"\",\"telefono\":\"abc\",\"categoria\":\"\",\"activo\":true}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/locales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Pasta Basta\",\"direccion\":\"Av. Italia 1200\",\"telefono\":\"+56912345678\",\"categoria\":\"Italiana\",\"activo\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Pasta Basta"));

        Pedido creado = localService.registrarPedido(new Pedido(999, "Felipe Diaz", "Pasta Basta", "1x Ravioli", 11500, "RECIBIDO_EN_COCINA"));

        mockMvc.perform(put("/api/locales/pedidos/" + creado.getId() + "/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"LISTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("LISTO"));
    }

    @Test
    @DisplayName("CocinaStreamConsumer procesa PedidoStreamEventDTO y lo registra en el servicio local")
    void testKafkaCocinaStreamConsumer() {
        com.pedidos360.local.dto.PedidoStreamEventDTO dto = new com.pedidos360.local.dto.PedidoStreamEventDTO(
                777, "Sofia Alarcon", "Pizzeria Napoli", "2x Pizza Margarita", "Av. Providencia 123", 16000, "CREADO", "2026-11-12", "2026-11-12T19:00:00"
        );

        cocinaStreamConsumer.procesarOrdenEnCocina(dto);

        List<Pedido> pedidos = localService.listarPedidosPorRestaurante("Pizzeria Napoli");
        assertTrue(pedidos.stream().anyMatch(p -> Integer.valueOf(777).equals(p.getPedidoId()) && "STREAM_EN_COCINA".equals(p.getEstado())));

        // Descarte controlado de payload nulo
        assertDoesNotThrow(() -> cocinaStreamConsumer.procesarOrdenEnCocina(null));
    }

    @Test
    @DisplayName("AuditoriaStreamConsumer procesa PedidoAuditDTO y tolera cargas nulas")
    void testKafkaAuditoriaStreamConsumer() {
        com.pedidos360.local.dto.PedidoAuditDTO audit = new com.pedidos360.local.dto.PedidoAuditDTO(
                "evt-123", "CREAR_PEDIDO", "PEDIDO", "888", "sistema", "2026-11-12T19:00:00", "Auditoria de streaming"
        );

        assertDoesNotThrow(() -> auditoriaStreamConsumer.procesarAuditoria(audit));
        assertDoesNotThrow(() -> auditoriaStreamConsumer.procesarAuditoria(null));
    }
}
