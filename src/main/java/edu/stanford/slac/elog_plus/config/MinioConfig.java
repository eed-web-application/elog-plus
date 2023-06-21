package edu.stanford.slac.elog_plus.config;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {
    MinioProperties minioProperties;

    public MinioConfig(MinioProperties minioProperties) {
        this.minioProperties = minioProperties;
    }

    @Bean
    public MinioClient objectStorageClient() throws Exception {
        return MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(
                        minioProperties.getKey(),
                        minioProperties.getSecret()
                )
                .build();
    }

    @Bean()
    public MinioProperties objectStorageProperties() {
        return minioProperties;
    }
}
