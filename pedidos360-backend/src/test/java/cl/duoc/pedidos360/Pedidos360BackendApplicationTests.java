package cl.duoc.pedidos360;

import cl.duoc.pedidos360.config.RabbitMQConfig;
import cl.duoc.pedidos360.models.Pedido;
import cl.duoc.pedidos360.service.OrderEventPublisher;
import cl.duoc.pedidos360.service.PedidoService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class Pedidos360BackendApplicationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private PedidoService pedidoService;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    @Autowired
    private RabbitMQConfig rabbitMQConfig;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    @DisplayName("El contexto de pedidos360-backend carga con configuracion segura sin credenciales reales")
    void contextLoads() {
        assertNotNull(pedidoService);
        assertNotNull(orderEventPublisher);
        DirectExchange exchange = rabbitMQConfig.ordersExchange();
        assertEquals(RabbitMQConfig.EXCHANGE_PEDIDOS, exchange.getName());
    }

    @Test
    @DisplayName("PedidoService guarda el pedido y publica evento completo hacia RabbitMQ incluyendo items y restaurante")
    @SuppressWarnings("unchecked")
    void testGuardarPedidoYPublicarEvento() {
        reset(rabbitTemplate);

        Pedido pedido = new Pedido();
        pedido.setCliente("Camila Soto");
        pedido.setRestaurante("Pizzeria Napoli");
        pedido.setItems("1x Pizza Margherita, 2x Bebida");
        pedido.setDireccion("Av. Providencia 1000");
        pedido.setMontoTotal(16500);
        pedido.setEstado("PENDIENTE");
        pedido.setFecha("2026-11-10");

        Pedido guardado = pedidoService.guardar(pedido);
        assertNotNull(guardado.getId());
        assertEquals("Camila Soto", guardado.getCliente());

        ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_PEDIDOS),
                eq(RabbitMQConfig.ROUTING_KEY_PEDIDO_CREADO),
                eventCaptor.capture()
        );

        Map<String, Object> evento = eventCaptor.getValue();
        assertEquals("PEDIDO_CREADO", evento.get("evento"));
        assertEquals("Pizzeria Napoli", evento.get("restaurante"));
        assertEquals("1x Pizza Margherita, 2x Bebida", evento.get("items"));
        assertEquals(16500, evento.get("montoTotal"));
    }

    @Test
    @DisplayName("Resiliencia: Si RabbitMQ falla al publicar, la persistencia en BD no se revierte")
    void testResilienciaAnteCaidaDeRabbitMQ() {
        reset(rabbitTemplate);
        doThrow(new AmqpException("Broker no disponible simulado"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        Pedido pedido = new Pedido();
        pedido.setCliente("Diego Rojas");
        pedido.setRestaurante("Burger House");
        pedido.setItems("1x Combo Doble");
        pedido.setDireccion("Calle Los Leones 200");
        pedido.setMontoTotal(9900);
        pedido.setEstado("PENDIENTE");
        pedido.setFecha("2026-11-10");

        Pedido guardado = assertDoesNotThrow(() -> pedidoService.guardar(pedido));
        assertNotNull(guardado.getId());
        assertEquals("Diego Rojas", pedidoService.obtenerPorId(guardado.getId()).getCliente());
    }

    @Test
    @DisplayName("PedidoService actualiza, elimina y lanza 404 cuando el ID no existe")
    void testCrudActualizarYEliminar() {
        reset(rabbitTemplate);

        Pedido pedido = new Pedido();
        pedido.setCliente("Test CRUD");
        pedido.setRestaurante("Sushi Roll");
        pedido.setItems("1x Gohan");
        pedido.setMontoTotal(8500);
        pedido.setEstado("PENDIENTE");

        Pedido guardado = pedidoService.guardar(pedido);

        Pedido cambios = new Pedido();
        cambios.setCliente("Test CRUD Editado");
        cambios.setRestaurante("Sushi Roll");
        cambios.setItems("2x Gohan");
        cambios.setMontoTotal(17000);
        cambios.setEstado("EN CAMINO");

        Pedido actualizado = pedidoService.actualizar(guardado.getId(), cambios);
        assertEquals("EN CAMINO", actualizado.getEstado());
        assertEquals("2x Gohan", actualizado.getItems());

        String msg = pedidoService.eliminar(guardado.getId());
        assertTrue(msg.contains("eliminado"));

        assertThrows(ResponseStatusException.class, () -> pedidoService.obtenerPorId(guardado.getId()));
    }

    @Test
    @DisplayName("Integracion HTTP + OAuth2 JWT: /api/pedidos exige Bearer token y crea pedido con SCOPE_OT.Create")
    void testSeguridadJwtYEndpointsHttp() throws Exception {
        reset(rabbitTemplate);

        // Sin token debe rechazar con 401
        mockMvc.perform(get("/api/pedidos"))
                .andExpect(status().isUnauthorized());

        // Con JWT valido y scope OT.Create debe persistir pedido y emitir evento
        Jwt mockJwt = Jwt.withTokenValue("valid-jwt-token")
                .header("alg", "RS256")
                .claim("sub", "usuario-prueba")
                .claim("scp", "OT.Create")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(jwtDecoder.decode("valid-jwt-token")).thenReturn(mockJwt);

        mockMvc.perform(post("/api/pedidos")
                        .header("Authorization", "Bearer valid-jwt-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cliente\":\"Lucia Vega\",\"restaurante\":\"La Fabbrica\",\"items\":\"1x Lasagna\",\"direccion\":\"Av. Apoquindo 3400\",\"montoTotal\":12900,\"estado\":\"PENDIENTE\",\"fecha\":\"2026-11-10\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cliente").value("Lucia Vega"))
                .andExpect(jsonPath("$.restaurante").value("La Fabbrica"));

        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(RabbitMQConfig.EXCHANGE_PEDIDOS),
                eq(RabbitMQConfig.ROUTING_KEY_PEDIDO_CREADO),
                any(Map.class)
        );
    }
}
