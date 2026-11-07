package com.pedidos360.local.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "pedidos_locales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer pedidoId;

    @Column(nullable = false, length = 100)
    private String cliente;

    @Column(nullable = false, length = 100)
    private String restaurante;

    @Column(length = 500)
    private String items;

    private Integer montoTotal;

    @Column(nullable = false, length = 50)
    private String estado = "RECIBIDO";

    private LocalDateTime fecha = LocalDateTime.now();

    public Pedido(Integer pedidoId, String cliente, String restaurante, String items, Integer montoTotal, String estado) {
        this.pedidoId = pedidoId;
        this.cliente = cliente;
        this.restaurante = restaurante;
        this.items = items;
        this.montoTotal = montoTotal;
        this.estado = estado != null ? estado : "RECIBIDO";
        this.fecha = LocalDateTime.now();
    }
}
