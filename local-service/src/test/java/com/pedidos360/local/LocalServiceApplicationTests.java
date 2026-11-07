package com.pedidos360.local;

import com.pedidos360.local.controller.LocalController;
import com.pedidos360.local.dto.CreateLocalDTO;
import com.pedidos360.local.dto.UpdateEstadoPedidoDTO;
import com.pedidos360.local.models.Local;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.service.LocalService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class LocalServiceApplicationTests {

    @Autowired
    private LocalService localService;

    @Autowired
    private LocalController localController;

    @Test
    @DisplayName("El contexto de Spring Boot de local-service carga correctamente")
    void contextLoads() {
        assertNotNull(localService);
        assertNotNull(localController);
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
}
