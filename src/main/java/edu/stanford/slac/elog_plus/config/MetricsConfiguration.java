package edu.stanford.slac.elog_plus.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AllArgsConstructor
public class MetricsConfiguration {
    private final MeterRegistry meterRegistry;
    @Bean
    public Counter previewProcessedCounter() {
        return Counter
                .builder("elog_plus_preview_processing_event")
                .tag("operation", "processing")
                .tag("state", "success")
                .description("The number of preview processed successfully")
                .register(meterRegistry);
    }

    @Bean
    public Counter previewErrorsCounter() {
        return Counter
                .builder("elog_plus_preview_processing_event")
                .tag("operation", "processing")
                .tag("state", "failed")
                .description("The number of preview processed with errors")
                .register(meterRegistry);
    }

    @Bean
    public Counter previewSubmittedCounter() {
        return Counter
                .builder("elog_plus_preview_processing_event")
                .tag("operation", "submission")
                .tag("state", "success")
                .description("The number of preview request submitted")
                .register(meterRegistry);
    }
}
