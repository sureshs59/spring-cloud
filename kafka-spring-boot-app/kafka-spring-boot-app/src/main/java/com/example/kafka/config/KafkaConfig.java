package com.example.kafka.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import com.example.kafka.model.ClaimEvent;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.topic.claims}")
    private String claimsTopic;

    @Value("${kafka.topic.claims-processed}")
    private String processedClaimsTopic;

    /**
     * Kafka Admin - Create Topics
     */
    @Bean
    public KafkaAdmin admin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return new KafkaAdmin(configs);
    }

    /**
     * Create claims-events topic
     */
    @Bean
    public NewTopic claimsEventsTopic() {
        return TopicBuilder.name(claimsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * Create processed-claims topic
     */
    @Bean
    public NewTopic processedClaimsTopic() {
        return TopicBuilder.name(processedClaimsTopic)
            .partitions(3)
            .replicas(1)
            .build();
    }

    /**
     * Producer Configuration
     */
    @Bean
    public ProducerFactory<String, ClaimEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");              // Wait for all replicas
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);              // Retry 3 times
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); // Exactly-once
        
        // Performance settings
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);        // 16KB batches
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 100);          // Wait 100ms for batch
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); // Compression
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, ClaimEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Consumer Configuration
     */
    @Bean
    public ConsumerFactory<String, ClaimEvent> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "claims-processor-group");
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ClaimEvent.class.getName());
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        // Offset management
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Start from beginning
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);     // Auto commit offsets
        configProps.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);// Commit every 1 second
        
        // Performance
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);        // Process 100 at a time
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000);    // 30 second timeout
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, ClaimEvent>> 
        kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ClaimEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setCommonErrorHandler(kafkaErrorHandler());
        factory.setConcurrency(3); // 3 consumer threads
        factory.getContainerProperties().setPollTimeout(3000); // Poll timeout 3 seconds
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    /**
     * Error handling for consumers
     */
    @Bean
    public org.springframework.kafka.listener.CommonErrorHandler kafkaErrorHandler() {
        return new org.springframework.kafka.listener.DefaultErrorHandler();
    }
}