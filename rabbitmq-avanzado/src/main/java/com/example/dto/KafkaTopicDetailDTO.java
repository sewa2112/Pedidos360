package com.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO con el detalle de configuracion de un topico Kafka inspeccionado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KafkaTopicDetailDTO {

    private String name;
    private int partitions;
    private int replicationFactor;
    private boolean internal;
    private Map<String, String> configs;
}
