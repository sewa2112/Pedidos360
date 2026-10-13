package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBindingDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de cola inválido")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de exchange inválido")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    private String routingKey;
}

