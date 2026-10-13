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
public class CreateQueueDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre de la cola debe tener entre 3 y 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre de la cola solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_)"
    )
    private String name;

    @Builder.Default
    private boolean durable = true;
}

