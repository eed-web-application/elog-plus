package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.model.Attachment;
import lombok.AllArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
@AllArgsConstructor
public class KafkaConfig {
    private KafkaProperties kafkaProperties;
    @Bean
    public ConsumerFactory<String, Attachment> consumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Attachment.class)
        );
    }
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Attachment> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Attachment> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}