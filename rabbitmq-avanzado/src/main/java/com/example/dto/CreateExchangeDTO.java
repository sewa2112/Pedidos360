package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateExchangeDTO {

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre del exchange debe tener entre 3 y 50 caracteres")
    @Pattern(
        regexp = "^[a-zA-Z0-9._-]+$",
        message = "El nombre del exchange solo puede contener letras, números, puntos (.), guiones (-) y guiones bajos (_)"
    )
    private String name;

    @Pattern(regexp = "^(direct|topic|fanout|headers)$", message = "El tipo de exchange debe ser: direct, topic, fanout o headers")
    private String type = "direct";

    private boolean durable = true;

    public CreateExchangeDTO() {
    }

    public CreateExchangeDTO(String name, String type, boolean durable) {
        this.name = name;
        this.type = type != null ? type : "direct";
        this.durable = durable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
        private String type = "direct";
        private boolean durable = true;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder durable(boolean durable) {
            this.durable = durable;
            return this;
        }

        public CreateExchangeDTO build() {
            return new CreateExchangeDTO(name, type, durable);
        }
    }
}
