package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CreateBindingDTO {

    @NotBlank(message = "El nombre de la cola no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre de la cola debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de cola inválido")
    private String queueName;

    @NotBlank(message = "El nombre del exchange no puede estar vacío")
    @Size(min = 3, max = 50, message = "El nombre del exchange debe tener entre 3 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Nombre de exchange inválido")
    private String exchangeName;

    @NotBlank(message = "La routing key no puede estar vacía")
    @Size(min = 1, max = 100, message = "La routing key debe tener entre 1 y 100 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9._*#-]+$", message = "La routing key contiene caracteres no permitidos")
    private String routingKey;

    public CreateBindingDTO() {
    }

    public CreateBindingDTO(String queueName, String exchangeName, String routingKey) {
        this.queueName = queueName;
        this.exchangeName = exchangeName;
        this.routingKey = routingKey;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public void setExchangeName(String exchangeName) {
        this.exchangeName = exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public void setRoutingKey(String routingKey) {
        this.routingKey = routingKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String queueName;
        private String exchangeName;
        private String routingKey;

        public Builder queueName(String queueName) {
            this.queueName = queueName;
            return this;
        }

        public Builder exchangeName(String exchangeName) {
            this.exchangeName = exchangeName;
            return this;
        }

        public Builder routingKey(String routingKey) {
            this.routingKey = routingKey;
            return this;
        }

        public CreateBindingDTO build() {
            return new CreateBindingDTO(queueName, exchangeName, routingKey);
        }
    }
}
