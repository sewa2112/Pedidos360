package cl.duoc.pedidos360.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import cl.duoc.pedidos360.models.Pedido;

@Repository 
public interface PedidoRepository extends JpaRepository<Pedido, Integer>{
    
}
