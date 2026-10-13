package cl.duoc.pedidos360.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import cl.duoc.pedidos360.models.Pedido;
import cl.duoc.pedidos360.repository.PedidoRepository;

@Service 
public class PedidoService {
@Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private OrderEventPublisher orderEventPublisher;

    public List<Pedido> obtenerTodos() {
        return pedidoRepository.findAll();
    }

    public Pedido obtenerPorId(Integer id) {
        Pedido pedido = pedidoRepository.findById(id).orElse(null);
        if (pedido == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado con esa id");
        }
        return pedido;
    }

    public Pedido guardar(Pedido pedido) {
        try {
            Pedido pedidoGuardado = pedidoRepository.save(pedido);
            // Publica el evento de forma asíncrona y desacoplada a RabbitMQ
            orderEventPublisher.publishOrderCreated(pedidoGuardado);
            return pedidoGuardado;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Pedido actualizar(Integer id, Pedido detallesPedido) {
        Pedido pedidoExistente = pedidoRepository.findById(id).orElse(null);
        if (pedidoExistente == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado con esa id");
        } else {
            pedidoExistente.setCliente(detallesPedido.getCliente());
            pedidoExistente.setRestaurante(detallesPedido.getRestaurante());
            pedidoExistente.setItems(detallesPedido.getItems());
            pedidoExistente.setDireccion(detallesPedido.getDireccion());
            pedidoExistente.setMontoTotal(detallesPedido.getMontoTotal());
            pedidoExistente.setEstado(detallesPedido.getEstado());
            pedidoExistente.setFecha(detallesPedido.getFecha());
            return pedidoRepository.save(pedidoExistente);
        }
    }

    public String eliminar(Integer id) {
        if (pedidoRepository.existsById(id)) {
            pedidoRepository.deleteById(id);
            return "Pedido eliminado correctamente";
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado con esa id");
        }
    }
}
