package com.pedidos360.local.controller;

import com.pedidos360.local.dto.CreateLocalDTO;
import com.pedidos360.local.dto.UpdateEstadoPedidoDTO;
import com.pedidos360.local.models.Local;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.service.LocalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/api/locales")
public class LocalController {

    private final LocalService localService;

    public LocalController(LocalService localService) {
        this.localService = localService;
    }

    @GetMapping
    public ResponseEntity<List<Local>> listarLocales() {
        return ResponseEntity.ok(localService.listarLocales());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Local> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(localService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<Local> crearLocal(@Valid @RequestBody CreateLocalDTO dto) {
        Local creado = localService.crearLocal(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @GetMapping("/pedidos")
    public ResponseEntity<List<Pedido>> listarPedidos() {
        return ResponseEntity.ok(localService.listarPedidos());
    }

    @GetMapping("/{restaurante}/pedidos")
    public ResponseEntity<List<Pedido>> listarPedidosPorRestaurante(@PathVariable String restaurante) {
        return ResponseEntity.ok(localService.listarPedidosPorRestaurante(restaurante));
    }

    @PutMapping("/pedidos/{id}/estado")
    public ResponseEntity<Pedido> actualizarEstado(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEstadoPedidoDTO dto) {
        Pedido actualizado = localService.actualizarEstadoPedido(id, dto.getEstado());
        return ResponseEntity.ok(actualizado);
    }
}
