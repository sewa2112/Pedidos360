package cl.duoc.pedidos360.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * DTO tipado para streaming de ordenes hacia el topico pedidos-stream.
 */
public class PedidoStreamEventDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer pedidoId;
    private String cliente;
    private String restaurante;
    private String items;
    private String direccion;
    private Integer montoTotal;
    private String estado;
    private String fecha;
    private String timestamp;

    public PedidoStreamEventDTO() {
        this.timestamp = Instant.now().toString();
    }

    public PedidoStreamEventDTO(Integer pedidoId, String cliente, String restaurante,
                                String items, String direccion, Integer montoTotal,
                                String estado, String fecha) {
        this.pedidoId = pedidoId;
        this.cliente = cliente;
        this.restaurante = restaurante;
        this.items = items;
        this.direccion = direccion;
        this.montoTotal = montoTotal;
        this.estado = estado;
        this.fecha = fecha;
        this.timestamp = Instant.now().toString();
    }

    public Integer getPedidoId() {
        return pedidoId;
    }

    public void setPedidoId(Integer pedidoId) {
        this.pedidoId = pedidoId;
    }

    public String getCliente() {
        return cliente;
    }

    public void setCliente(String cliente) {
        this.cliente = cliente;
    }

    public String getRestaurante() {
        return restaurante;
    }

    public void setRestaurante(String restaurante) {
        this.restaurante = restaurante;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public Integer getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(Integer montoTotal) {
        this.montoTotal = montoTotal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
