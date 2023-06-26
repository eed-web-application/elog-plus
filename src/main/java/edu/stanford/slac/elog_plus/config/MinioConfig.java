package edu.stanford.slac.elog_plus.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
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
        MinioClient mc = MinioClient.builder()
                .endpoint(minioProperties.getUrl())
                .credentials(
                        minioProperties.getKey(),
                        minioProperties.getSecret()
                )
                .build();
        boolean exists = mc.bucketExists(
                BucketExistsArgs
                        .builder()
                        .bucket(minioProperties.getBucket())
                        .build()
        );
        if(!exists) {
            mc.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(minioProperties.getBucket())
                            .build());
        }
        return mc;
    }


    @Bean()
    public MinioProperties objectStorageProperties() {
        return minioProperties;
    }
}
