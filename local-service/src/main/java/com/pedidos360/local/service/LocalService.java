package com.pedidos360.local.service;

import com.pedidos360.local.config.RabbitMQConfig;
import com.pedidos360.local.dto.CreateLocalDTO;
import com.pedidos360.local.models.Local;
import com.pedidos360.local.models.Pedido;
import com.pedidos360.local.repository.LocalRepository;
import com.pedidos360.local.repository.PedidoRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LocalService {

    private static final Logger log = LoggerFactory.getLogger(LocalService.class);

    private final LocalRepository localRepository;
    private final PedidoRepository pedidoRepository;
    private final RabbitTemplate rabbitTemplate;

    public LocalService(LocalRepository localRepository,
                        PedidoRepository pedidoRepository,
                        RabbitTemplate rabbitTemplate) {
        this.localRepository = localRepository;
        this.pedidoRepository = pedidoRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void seedInitialLocales() {
        if (localRepository.count() == 0) {
            localRepository.save(new Local("Pizzeria Napoli", "Av. Providencia 1234", "+56911112222", "Italiana", true));
            localRepository.save(new Local("Burger House", "Calle Los Leones 450", "+56922223333", "Hamburguesas", true));
            localRepository.save(new Local("Sushi Roll", "Av. Las Condes 8900", "+56933334444", "Japonesa", true));
            localRepository.save(new Local("Taco Express", "Manuel Montt 620", "+56944445555", "Mexicana", true));
            log.info("Locales iniciales sembrados en la base de datos de local-service");
        }
    }

    public List<Local> listarLocales() {
        return localRepository.findAll();
    }

    public Local obtenerPorId(Long id) {
        return localRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Local no encontrado con id: " + id));
    }

    public Local crearLocal(CreateLocalDTO dto) {
        Local local = new Local(dto.getNombre(), dto.getDireccion(), dto.getTelefono(), dto.getCategoria(), dto.isActivo());
        return localRepository.save(local);
    }

    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    public List<Pedido> listarPedidosPorRestaurante(String restaurante) {
        return pedidoRepository.findByRestauranteIgnoreCase(restaurante);
    }

    @Transactional
    public Pedido registrarPedido(Pedido pedido) {
        Pedido guardado = pedidoRepository.save(pedido);
        log.info("Pedido registrado en cocina del local: ID={}, Restaurante={}, PedidoId={}",
                guardado.getId(), guardado.getRestaurante(), guardado.getPedidoId());
        return guardado;
    }

    @Transactional
    public Pedido actualizarEstadoPedido(Long pedidoLocalId, String nuevoEstado) {
        Pedido pedido = pedidoRepository.findById(pedidoLocalId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado con id: " + pedidoLocalId));

        pedido.setEstado(nuevoEstado);
        Pedido actualizado = pedidoRepository.save(pedido);

        // Notificar cambio de estado hacia RabbitMQ
        try {
            Map<String, Object> evento = new HashMap<>();
            evento.put("evento", "ESTADO_COCINA_ACTUALIZADO");
            evento.put("pedidoLocalId", pedido.getId());
            evento.put("pedidoId", pedido.getPedidoId());
            evento.put("restaurante", pedido.getRestaurante());
            evento.put("nuevoEstado", nuevoEstado);

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_RESTAURANT,
                    RabbitMQConfig.ROUTING_KEY_STATUS_UPDATED,
                    evento
            );
            log.info("Evento de estado de pedido emitido hacia RabbitMQ: {}", evento);
        } catch (Exception e) {
            log.warn("No se pudo notificar el cambio de estado a RabbitMQ: {}", e.getMessage());
        }

        return actualizado;
    }
}
