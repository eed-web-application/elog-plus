package edu.stanford.slac.elog_plus.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class MinioConfig {
    AppProperties appProperties;

    public MinioConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public MinioClient objectStorageClient() throws Exception {
        MinioClient mc = MinioClient.builder()
                .endpoint(appProperties.getMinio().getUrl())
                .credentials(
                        appProperties.getMinio().getKey(),
                        appProperties.getMinio().getSecret()
                )
                .build();
        boolean exists = mc.bucketExists(
                BucketExistsArgs
                        .builder()
                        .bucket(appProperties.getMinio().getBucket())
                        .build()
        );
        if(!exists) {
            mc.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(appProperties.getMinio().getBucket())
                            .build());
        }
        return mc;
    }


    @Bean()
    public MinioProperties objectStorageProperties() {
        return appProperties.getMinio();
    }
}
