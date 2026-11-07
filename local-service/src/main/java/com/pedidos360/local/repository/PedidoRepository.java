package com.pedidos360.local.repository;

import com.pedidos360.local.models.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
    List<Pedido> findByRestauranteIgnoreCase(String restaurante);
    List<Pedido> findByEstado(String estado);
}
