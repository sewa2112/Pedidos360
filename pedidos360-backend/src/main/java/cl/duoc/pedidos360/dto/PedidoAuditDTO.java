package cl.duoc.pedidos360.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * DTO tipado para streaming de auditoria y trazabilidad hacia el topico pedidos-audit-stream.
 */
public class PedidoAuditDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String eventoId;
    private String tipoEvento;
    private String entidad;
    private String idEntidad;
    private String usuario;
    private String timestamp;
    private String detalle;

    public PedidoAuditDTO() {
        this.eventoId = UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
    }

    public PedidoAuditDTO(String tipoEvento, String entidad, String idEntidad,
                          String usuario, String detalle) {
        this.eventoId = UUID.randomUUID().toString();
        this.tipoEvento = tipoEvento;
        this.entidad = entidad;
        this.idEntidad = idEntidad;
        this.usuario = usuario;
        this.detalle = detalle;
        this.timestamp = Instant.now().toString();
    }

    public String getEventoId() {
        return eventoId;
    }

    public void setEventoId(String eventoId) {
        this.eventoId = eventoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getEntidad() {
        return entidad;
    }

    public void setEntidad(String entidad) {
        this.entidad = entidad;
    }

    public String getIdEntidad() {
        return idEntidad;
    }

    public void setIdEntidad(String idEntidad) {
        this.idEntidad = idEntidad;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getDetalle() {
        return detalle;
    }

    public void setDetalle(String detalle) {
        this.detalle = detalle;
    }
}
