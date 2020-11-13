package com.github.j3t.mvnio.storage;

import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

import static com.github.j3t.mvnio.storage.S3CredentialsWebFilter.S3_CREDENTIALS_PROVIDER;

import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.j3t.mvnio.error.NotAuthorizedException;

import reactor.test.StepVerifier;
import reactor.util.context.Context;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import testcontainers.MinioContainer;
import testcontainers.MinioMcContainer;

@Testcontainers
class S3RepositoryTest {

    @Container
    static MinioContainer minio = new MinioContainer();

    @Container
    static MinioMcContainer mc = new MinioMcContainer(minio);

    S3Repository s3Repository = new S3RepositoryS3AsyncClientImpl(S3AsyncClient.builder()
            .region(Region.US_EAST_1)
            .endpointOverride(URI.create(minio.getExternalAddress()))
            .build());

    @BeforeAll
    static void initTestBucket() throws Exception {
        // GIVEN
        mc.createBucket("releases");
        mc.createObject("releases", "a/a/1.0.0/a-1.0.0.pom");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.pom");
        mc.createObject("releases", "a/c/1.0.0/c-1.0.0.pom");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.jar");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.bundle");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.pom.sha1");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.jar.sha1");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.bundle.sha1");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.pom.md5");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.jar.md5");
        mc.createObject("releases", "a/b/1.0.0/b-1.0.0.bundle.md5");
        mc.createObject("releases", "a/a/maven-metadata.xml");
        mc.createObject("releases", "a/b/maven-metadata.xml");
        mc.createObject("releases", "a/c/maven-metadata.xml");
        mc.createObject("releases", "a/c/1.0.0-SNAPSHOT/c-1.0.0-20201112.210809-1.pom");
        mc.createObject("releases", "a/c/1.0.0-SNAPSHOT/maven-metadata.xml");
        mc.createObject("releases", "a/c/maven-metadata.xml");
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
                        .contextWrite(this::injectCredentials))

                // THEN
                .verifyError(NoSuchKeyException.class); // unfortunately, S3 throws NoSuchKeyException instead of NoSuchBucketException
    }

    @Test
    void testHeadObjectNotFound() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .head("releases", randomAlphanumeric(32))
                        .contextWrite(this::injectCredentials))

                // THEN
                .verifyError(NoSuchKeyException.class);
    }

    @Test
    void testHeadObjectExists() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .head("releases", "a/a/1.0.0/a-1.0.0.pom")
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void testMetadataGetAll() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .metadata("releases", null, Integer.MAX_VALUE)
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("/a/a/maven-metadata.xml","/a/b/maven-metadata.xml","/a/c/maven-metadata.xml")
                .verifyComplete();
    }

    @Test
    void testMetadataLimit() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .metadata("releases", null, 2)
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("/a/a/maven-metadata.xml","/a/b/maven-metadata.xml")
                .verifyComplete();
    }

    @Test
    void testMetadataStartAfter() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .metadata("releases", "a/b/maven-metadata.xml", 2)
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("/a/c/maven-metadata.xml")
                .verifyComplete();
    }

    @Test
    void testListDirectories() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .list("releases", "/a")
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("a/", "b/", "c/")
                .verifyComplete();
    }

    @Test
    void testListFiles() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .list("releases", "/a/a/1.0.0")
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("a-1.0.0.pom")
                .verifyComplete();
    }

    @Test
    void testListFilesAndDirectories() {
        // WHEN
        StepVerifier
                .create(s3Repository
                        .list("releases", "/a/b")
                        .contextWrite(this::injectCredentials))

                // THEN
                .expectNext("1.0.0/", "maven-metadata.xml")
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