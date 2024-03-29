package com.github.j3t.mvnio;

import java.time.Duration;

import com.github.j3t.mvnio.storage.S3CredentialsWebFilter;
import com.github.j3t.mvnio.storage.S3Repository;
import com.github.j3t.mvnio.storage.S3RepositoryS3AsyncClientImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3AsyncClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;

@Slf4j
@SpringBootApplication
public class App {

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    AppProperties appProperties() {
        var appProperties = new AppProperties();
        log.info("appProperties: " + appProperties);
        return appProperties;
    }

    @Bean
    S3AsyncClient s3client(AppProperties appProperties) {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ZERO)
                .maxConcurrency(64)
                .build();

        S3Configuration serviceConfiguration = S3Configuration.builder()
                .checksumValidationEnabled(true)
                .chunkedEncodingEnabled(true)
                .pathStyleAccessEnabled(true)
                .build();

        S3AsyncClientBuilder builder = S3AsyncClient.builder()
                .httpClient(httpClient)
                .region(Region.of(appProperties.getS3Region()))
                .serviceConfiguration(serviceConfiguration);

        if (appProperties.isS3OverrideEndpoint()) {
            builder.endpointOverride(appProperties.getS3Endpoint());
        }

        return builder.build();
    }

    @Bean
    S3Repository s3Repository(S3AsyncClient s3client) {
        return new S3RepositoryS3AsyncClientImpl(s3client);
    }

    @Bean
    S3CredentialsWebFilter credentialContextFilter() {
        return new S3CredentialsWebFilter();
    }

}
