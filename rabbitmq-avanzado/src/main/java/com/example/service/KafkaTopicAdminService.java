package com.example.service;

import com.example.dto.CreateKafkaTopicDTO;
import com.example.dto.KafkaTopicDetailDTO;
import org.apache.kafka.clients.admin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la gestion y administracion de topicos y consumer groups en Apache Kafka.
 */
@Service
public class KafkaTopicAdminService {

    private static final Logger log = LoggerFactory.getLogger(KafkaTopicAdminService.class);

    private final AdminClient adminClient;

    public KafkaTopicAdminService(AdminClient adminClient) {
        this.adminClient = adminClient;
    }

    public List<String> listTopics() {
        try {
            ListTopicsResult result = adminClient.listTopics(new ListTopicsOptions().listInternal(false));
            Set<String> names = result.names().get(5, TimeUnit.SECONDS);
            return new ArrayList<>(names);
        } catch (Exception e) {
            log.error("Error al listar topicos en Kafka: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean createTopic(CreateKafkaTopicDTO dto) {
        try {
            NewTopic newTopic = new NewTopic(dto.getName(), dto.getPartitions(), dto.getReplicationFactor());
            Map<String, String> configs = dto.toTopicConfigs();
            if (!configs.isEmpty()) {
                newTopic.configs(configs);
            }

            CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
            result.all().get(5, TimeUnit.SECONDS);
            log.info("Topico Kafka [{}] creado exitosamente con {} particiones y factor {}",
                    dto.getName(), dto.getPartitions(), dto.getReplicationFactor());
            return true;
        } catch (Exception e) {
            log.error("Error al crear topico [{}] en Kafka: {}", dto.getName(), e.getMessage());
            throw new RuntimeException("No se pudo crear el topico Kafka: " + e.getMessage(), e);
        }
    }

    public KafkaTopicDetailDTO describeTopic(String topicName) {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topicName));
            TopicDescription desc = result.values().get(topicName).get(5, TimeUnit.SECONDS);

            int partitions = desc.partitions().size();
            int replication = partitions > 0 ? desc.partitions().get(0).replicas().size() : 1;

            return new KafkaTopicDetailDTO(
                    desc.name(),
                    partitions,
                    replication,
                    desc.isInternal(),
                    Collections.emptyMap()
            );
        } catch (Exception e) {
            log.error("Error al describir topico [{}]: {}", topicName, e.getMessage());
            throw new RuntimeException("Topico Kafka no encontrado o no disponible: " + topicName, e);
        }
    }

    public boolean deleteTopic(String topicName) {
        try {
            DeleteTopicsResult result = adminClient.deleteTopics(Collections.singletonList(topicName));
            result.all().get(5, TimeUnit.SECONDS);
            log.info("Topico Kafka [{}] eliminado exitosamente", topicName);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar topico [{}] en Kafka: {}", topicName, e.getMessage());
            throw new RuntimeException("No se pudo eliminar el topico Kafka: " + e.getMessage(), e);
        }
    }

    public List<String> listConsumerGroups() {
        try {
            ListConsumerGroupsResult result = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groups = result.all().get(5, TimeUnit.SECONDS);
            return groups.stream().map(ConsumerGroupListing::groupId).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error al listar grupos de consumidores en Kafka: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}
