package com.example.controller;

import com.example.dto.CreateKafkaTopicDTO;
import com.example.dto.KafkaTopicDetailDTO;
import com.example.service.KafkaTopicAdminService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST para administracion de topicos y consumer groups de Apache Kafka.
 * Cumple con los criterios de administracion REST de mensajeria y streaming.
 */
@RestController
@RequestMapping("/api/kafka")
@CrossOrigin(origins = "*")
public class KafkaTopicAdminController {

    private final KafkaTopicAdminService kafkaTopicAdminService;

    public KafkaTopicAdminController(KafkaTopicAdminService kafkaTopicAdminService) {
        this.kafkaTopicAdminService = kafkaTopicAdminService;
    }

    @GetMapping("/topics")
    public ResponseEntity<List<String>> listTopics() {
        return ResponseEntity.ok(kafkaTopicAdminService.listTopics());
    }

    @PostMapping("/topics")
    public ResponseEntity<Map<String, Object>> createTopic(@Valid @RequestBody CreateKafkaTopicDTO dto) {
        kafkaTopicAdminService.createTopic(dto);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "CREATED");
        response.put("topic", dto.getName());
        response.put("partitions", dto.getPartitions());
        response.put("replicationFactor", dto.getReplicationFactor());
        response.put("retentionMs", dto.getRetentionMs());
        response.put("cleanupPolicy", dto.getCleanupPolicy());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/topics/{topicName}")
    public ResponseEntity<KafkaTopicDetailDTO> describeTopic(@PathVariable String topicName) {
        return ResponseEntity.ok(kafkaTopicAdminService.describeTopic(topicName));
    }

    @DeleteMapping("/topics/{topicName}")
    public ResponseEntity<Map<String, String>> deleteTopic(@PathVariable String topicName) {
        kafkaTopicAdminService.deleteTopic(topicName);

        Map<String, String> response = new HashMap<>();
        response.put("status", "DELETED");
        response.put("topic", topicName);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/consumer-groups")
    public ResponseEntity<List<String>> listConsumerGroups() {
        return ResponseEntity.ok(kafkaTopicAdminService.listConsumerGroups());
    }
}
