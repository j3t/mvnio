package com.github.j3t.mvnio;

import com.github.j3t.mvnio.repo.AWSContextFilter;
import com.github.j3t.mvnio.storage.S3Repository;
import com.github.j3t.mvnio.storage.S3RepositoryS3AsyncClientImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;

@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    AppProperties appProperties() {
        return new AppProperties();
    }

    @Bean
    S3AsyncClient s3client(AppProperties appProperties) {
        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(true)
                .chunkedEncodingEnabled(true)
                .build();

        return S3AsyncClient.builder()
                .region(Region.of(appProperties.getS3Region()))
                .serviceConfiguration(serviceConfiguration)
                .endpointOverride(appProperties.getS3Endpoint())
                .build();
    }

    @Bean
    S3Repository s3Repository(S3AsyncClient s3client) {
        return new S3RepositoryS3AsyncClientImpl(s3client);
    }

    @Bean
    AWSContextFilter credentialContextFilter() {
        return new AWSContextFilter();
    }

}
