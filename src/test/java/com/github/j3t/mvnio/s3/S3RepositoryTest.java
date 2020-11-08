package com.github.j3t.mvnio.s3;

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import static com.github.j3t.mvnio.s3.S3CredentialsWebFilter.S3_CREDENTIALS_PROVIDER;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.j3t.mvnio.error.NotAuthorizedException;

import reactor.test.StepVerifier;
import reactor.util.context.Context;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import testcontainers.MinioContainer;
import testcontainers.MinioMcContainer;

@SpringBootTest
@Testcontainers
class S3RepositoryTest {

    @Container
    static MinioContainer minio = new MinioContainer();

    @Container
    static MinioMcContainer mc = new MinioMcContainer(minio);

    @Autowired
    S3Repository s3Repository;

    @DynamicPropertySource
    static void appProperties(DynamicPropertyRegistry registry) {
        registry.add("s3.override-endpoint", () -> true);
        registry.add("s3.endpoint", () -> minio.getExternalAddress());
    }

    @BeforeEach
    void initTestBucket() throws Exception {
        // GIVEN
        mc.deleteBucket("releases");
        mc.createBucket("releases");
    }

    @Test
    void testHeadUnauthorized() {
        // WHEN
        StepVerifier.create(s3Repository.head("releases", randomAlphanumeric(32)))

                // THEN
                .verifyError(NotAuthorizedException.class);
    }

    @Test
    void testHeadBucketNotFound() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .head(randomAlphabetic(10), randomAlphanumeric(32))
                        .subscriberContext(this::injectCredentials))

                // THEN
                .verifyError(NoSuchKeyException.class); // unfortunately, S3 throws NoSuchKeyException instead of NoSuchBucketException
    }

    @Test
    void testHeadObjectNotFound() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .head("releases", randomAlphanumeric(32))
                        .subscriberContext(this::injectCredentials))

                // THEN
                .verifyError(NoSuchKeyException.class);
    }

    @Test
    void testHeadObjectExists() throws IOException, InterruptedException {
        // GIVEN
        mc.createObject("releases", "pom.xml", "<a></a>");

        // WHEN
        StepVerifier
                .create(s3Repository
                        .head("releases", "pom.xml")
                        .subscriberContext(this::injectCredentials))

                // THEN
                .expectNextCount(1)
                .verifyComplete();
    }

    // upload and download are already covered by com.github.j3t.mvnio.AppTests

    private AwsCredentialsProvider createCredentialsProvider() {
        return () -> AwsBasicCredentials.create(minio.accessKey(), minio.secretKey());
    }

    private Context injectCredentials(Context context) {
        return context.put(S3_CREDENTIALS_PROVIDER, createCredentialsProvider());
    }

}