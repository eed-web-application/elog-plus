package edu.stanford.slac.elog_plus.config;

import edu.stanford.slac.elog_plus.model.Attachment;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.Map;

@Configuration
@AllArgsConstructor
@AutoConfigureBefore(KafkaAutoConfiguration.class)
public class KafkaConfig {
    private MeterRegistry meterRegistry;
    private KafkaProperties kafkaProperties;

    @Bean
    public ConsumerFactory<String, Attachment> consumerFactory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        DefaultKafkaConsumerFactory<String, Attachment> cf = new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                new JsonDeserializer<>(Attachment.class)
        );
        cf.addListener(new MicrometerConsumerListener<>(meterRegistry));
        return cf;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Attachment> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Attachment> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public ProducerFactory<String, Attachment> producerFactory() {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        DefaultKafkaProducerFactory<String, Attachment> pf = new DefaultKafkaProducerFactory<>(props);
        pf.addListener(new MicrometerProducerListener<>(meterRegistry));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, Attachment> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}