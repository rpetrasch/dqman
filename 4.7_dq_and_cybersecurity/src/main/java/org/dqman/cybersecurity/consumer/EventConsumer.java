package org.dqman.cybersecurity.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.dqman.cybersecurity.model.RawLogEvent;
import org.dqman.cybersecurity.pipeline.EventPipelineService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Four @KafkaListener methods — one per input topic — deserializing each into a
 * typed RawLogEvent POJO before the shared normalization pipeline.
 * One consumer group (pipeline-service) handles all four topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final EventPipelineService pipeline;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.topics.email}", groupId = "pipeline-service")
    public void consumeEmailLog(ConsumerRecord<String, String> record) {
        process(record, "EMAIL");
    }

    @KafkaListener(topics = "${app.kafka.topics.idp}", groupId = "pipeline-service")
    public void consumeIdpLog(ConsumerRecord<String, String> record) {
        process(record, "IDP");
    }

    @KafkaListener(topics = "${app.kafka.topics.network}", groupId = "pipeline-service")
    public void consumeNetworkLog(ConsumerRecord<String, String> record) {
        process(record, "NETWORK");
    }

    @KafkaListener(topics = "${app.kafka.topics.geolocation}", groupId = "pipeline-service")
    public void consumeGeolocationLog(ConsumerRecord<String, String> record) {
        process(record, "GEOLOCATION");
    }

    private void process(ConsumerRecord<String, String> record, String expectedSource) {
        try {
            RawLogEvent event = objectMapper.readValue(record.value(), RawLogEvent.class);
            // Ingestion time is when Kafka received the record, not when we consume it
            if (event.getIngestionTime() == null) {
                event.setIngestionTime(Instant.ofEpochMilli(record.timestamp()));
            }
            event.setSourceSystem(expectedSource);
            event.setSourceOffset(record.offset());
            event.setSourceFile(record.topic() + "[" + record.partition() + "]");

            pipeline.process(event);
        } catch (Exception e) {
            log.error("Failed to process {} event from offset {}: {}", expectedSource, record.offset(), e.getMessage());
        }
    }
}
