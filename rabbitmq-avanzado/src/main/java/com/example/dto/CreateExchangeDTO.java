package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExchangeDTO {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre del exchange debe tener entre 3 y 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre del exchange solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_)"
    )
    private String name;

    @Builder.Default
    @Pattern(regexp = "^(direct|topic|fanout|headers)$", message = "El tipo de exchange debe ser: direct, topic, fanout o headers")
    private String type = "direct";

    @Builder.Default
    private boolean durable = true;
}

