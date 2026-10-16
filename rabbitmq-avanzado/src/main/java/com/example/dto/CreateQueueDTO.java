package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateQueueDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre de la cola debe tener entre 3 y 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre de la cola solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_)"
    )
    private String name;

    private boolean durable = true;

    public CreateQueueDTO() {
    }

    public CreateQueueDTO(String name, boolean durable) {
        this.name = name;
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDurable() {
        return durable;
    }

    public void setDurable(boolean durable) {
        this.durable = durable;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String name;
        private boolean durable = true;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder durable(boolean durable) {
            this.durable = durable;
            return this;
        }

        public CreateQueueDTO build() {
            return new CreateQueueDTO(name, durable);
        }
    }
}
