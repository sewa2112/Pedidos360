package com.example.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * DTO para la creacion administrada de topicos Kafka con parametros de
 * particiones, factor de replicacion y politicas de retencion.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateKafkaTopicDTO {

    @NotBlank(message = "El nombre del topico no puede estar vacio")
    private String name;

    @Min(value = 1, message = "Debe especificarse al menos 1 particion")
    private int partitions = 1;

    @Min(value = 1, message = "El factor de replicacion debe ser al menos 1")
    private short replicationFactor = 1;

    /**
     * Tiempo de retencion en milisegundos (ej: 604800000 = 7 dias, 2592000000 = 30 dias).
     */
    private Long retentionMs = 604800000L;

    /**
     * Politica de limpieza: "delete" o "compact".
     */
    private String cleanupPolicy = "delete";

    public Map<String, String> toTopicConfigs() {
        Map<String, String> configs = new HashMap<>();
        if (retentionMs != null) {
            configs.put("retention.ms", String.valueOf(retentionMs));
        }
        if (cleanupPolicy != null && !cleanupPolicy.isBlank()) {
            configs.put("cleanup.policy", cleanupPolicy);
        }
        return configs;
    }
}
