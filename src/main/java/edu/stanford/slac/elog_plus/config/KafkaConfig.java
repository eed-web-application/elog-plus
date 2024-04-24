package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.api.v2.dto.ImportEntryDTO;
import edu.stanford.slac.elog_plus.model.Attachment;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Log4j2
@Configuration
@RequiredArgsConstructor
@AutoConfigureBefore(KafkaAutoConfiguration.class)
public class KafkaConfig {
    private final MeterRegistry meterRegistry;
    private final KafkaProperties kafkaProperties;
    @Value("${edu.stanford.slac.elog-plus.kafka-consumer-concurrency}")
    private int concurrencyLevel = 1;

    @Bean
    public DefaultErrorHandler errorHandler() {
        return new DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    if (exception.getCause() instanceof DeserializationException) {
                        System.err.println("Deserialization error for record: " + consumerRecord);
                        System.err.println("Exception message: " + exception.getMessage());
                    }
                }
        );
    }

    @Bean
    public ConsumerFactory<String, Attachment> attachmentKafkaListenerConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        // Calculate max poll records based on concurrency level
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2 * concurrencyLevel);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());  // Replace JsonDeserializer with your key deserializer if different
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        DefaultKafkaConsumerFactory<String, Attachment> cf = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Attachment.class, false)
        );
        cf.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return cf;
    }

    @Bean
    public ConsumerFactory<String, ImportEntryDTO> importEntryKafkaListenerConsumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();

        // Calculate max poll records based on concurrency level
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 2 * concurrencyLevel);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class.getName());  // Replace JsonDeserializer with your key deserializer if different
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        DefaultKafkaConsumerFactory<String, ImportEntryDTO> cf = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(ImportEntryDTO.class, false)
        );
        cf.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return cf;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Attachment> attachmentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Attachment> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(attachmentKafkaListenerConsumerFactory());
        factory.setConcurrency(concurrencyLevel);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);  // Set AckMode to MANUAL
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ImportEntryDTO> importEntryKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ImportEntryDTO> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(importEntryKafkaListenerConsumerFactory());
        factory.setConcurrency(concurrencyLevel);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);  // Set AckMode to MANUAL
        return factory;
    }

    @Bean
    public ProducerFactory<String, Attachment> attachementProducerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        DefaultKafkaProducerFactory<String, Attachment> pf = new DefaultKafkaProducerFactory<>(props);
        pf.addListener(new MicrometerProducerListener<>(meterRegistry));
        return pf;
    }

    @Bean
    public ProducerFactory<String, ImportEntryDTO> importEntryProducerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        DefaultKafkaProducerFactory<String, ImportEntryDTO> pf = new DefaultKafkaProducerFactory<>(props);
        pf.addListener(new MicrometerProducerListener<>(meterRegistry));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, Attachment> attachmentKafkaTemplate() {
        return new KafkaTemplate<>(attachementProducerFactory());
    }

    @Bean
    public KafkaTemplate<String, ImportEntryDTO> importEntryDTOKafkaTemplate() {
        return new KafkaTemplate<>(importEntryProducerFactory());
    }
}