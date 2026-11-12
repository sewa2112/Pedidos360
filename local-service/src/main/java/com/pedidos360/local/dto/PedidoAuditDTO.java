package com.pedidos360.local.dto;

import java.io.Serializable;

/**
 * DTO para consumir eventos desde el topico pedidos-audit-stream.
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

    public PedidoAuditDTO() {}

    public PedidoAuditDTO(String eventoId, String tipoEvento, String entidad,
                          String idEntidad, String usuario, String timestamp, String detalle) {
        this.eventoId = eventoId;
        this.tipoEvento = tipoEvento;
        this.entidad = entidad;
        this.idEntidad = idEntidad;
        this.usuario = usuario;
        this.timestamp = timestamp;
        this.detalle = detalle;
    }

    public String getEventoId() { return eventoId; }
    public void setEventoId(String eventoId) { this.eventoId = eventoId; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getEntidad() { return entidad; }
    public void setEntidad(String entidad) { this.entidad = entidad; }
    public String getIdEntidad() { return idEntidad; }
    public void setIdEntidad(String idEntidad) { this.idEntidad = idEntidad; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String detalle) { this.detalle = detalle; }
}
