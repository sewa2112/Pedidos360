package com.pedidos360.local.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UpdateEstadoPedidoDTO {

    @NotBlank(message = "El estado no puede estar vacio")
    @Pattern(
        regexp = "^(RECIBIDO|EN_PREPARACION|LISTO|ENTREGADO)$",
        message = "El estado debe ser: RECIBIDO, EN_PREPARACION, LISTO o ENTREGADO"
    )
    private String estado;

    public UpdateEstadoPedidoDTO() {
    }

    public UpdateEstadoPedidoDTO(String estado) {
        this.estado = estado;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
