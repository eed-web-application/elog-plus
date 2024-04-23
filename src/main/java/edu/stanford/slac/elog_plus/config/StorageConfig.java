package edu.stanford.slac.elog_plus.config;

import lombok.AllArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(ELOGAppProperties.class)
@AllArgsConstructor
public class StorageConfig {
    private final ELOGAppProperties elogAppProperties;

    @Bean
    public S3Client s3Client() {
        Region region = Region.US_EAST_1;
        S3Client s3 = S3Client.builder()
                .region(region)
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        elogAppProperties.getStorage().getKey(),
                                        elogAppProperties.getStorage().getSecret()
                                )
                        )
                )
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .endpointOverride(URI.create(elogAppProperties.getStorage().getUrl()))
                .build();
        if (!doesBucketExist(
                s3,
                elogAppProperties.getStorage().getBucket())) {
            s3.createBucket(
                    CreateBucketRequest
                            .builder()
                            .bucket(elogAppProperties.getStorage().getBucket())
                            .build()
            );
        }
        return s3;
    }

    private boolean doesBucketExist(S3Client s3, String bucketName) {
        try {
            s3.headBucket(
                    HeadBucketRequest.builder()
                            .bucket(bucketName)
                            .build()
            );
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }


    @Bean()
    public StorageProperties objectStorageProperties() {
        return elogAppProperties.getStorage();
    }
}
