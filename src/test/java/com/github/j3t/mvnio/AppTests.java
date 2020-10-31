package com.github.j3t.mvnio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testcontainers.MinioContainer;
import testcontainers.MinioMcContainer;

import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.util.Base64Utils.encodeToString;

/**
 * Checks that basic functionality works as expected. The test environment consists of a webserver which actually
 * implements the repository and a minio container which is used to store the data. Before each test, an empty
 * bucket named "releases" gets created. The tests are written by using the GIVEN/WHEN/THEN approach and test data
 * is generated with the mc cli.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AppTests {

    public static final String EXPECTED_XML = "<abc></abc>";
    @Container
    static MinioContainer minio = new MinioContainer();

    @Container
    static MinioMcContainer mc = new MinioMcContainer(minio);

    @Autowired
    WebTestClient webTestClient;

    @DynamicPropertySource
    static void appProperties(DynamicPropertyRegistry registry) {
        registry.add("s3.override-endpoint", () -> true);
        registry.add("s3.endpoint", () -> minio.getExternalAddress());
    }

    @BeforeEach
    void initTestBucket() throws Exception {
        mc.deleteBucket("releases");
        mc.createBucket("releases");
    }

    @Test
    void testAuthenticateChallenge() {
        // GIVEN

        // WHEN
        webTestClient.get()
                .uri("/maven/releases")
                .exchange()

                // THEN
                .expectStatus().isUnauthorized()
                .expectHeader().exists("WWW-Authenticate");
    }

    @Test
    void testUploadRepositoryNotFound() {
        // GIVEN

        // WHEN
        uploadExchange(UUID.randomUUID().toString(), "/bla/foo/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testUploadSnapshotRepositoryNotFound() {
        // GIVEN

        // WHEN
        uploadExchange("snapshots", "/bla/foo/1.0.1-SNAPSHOT/foo-1.0.1-20201023.142512-1.jar")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testDownloadRepositoryNotFound() {
        // GIVEN

        // WHEN
        downloadExchange(UUID.randomUUID().toString(), "/bla/foo/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testArtifactNotFound() {
        // GIVEN

        // WHEN
        downloadExchange("/bla/foo/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testArtifactIsUploadedAndAvailable() {
        // GIVEN
        uploadExchange("/bla/foo/1.0.1/foo-1.0.1.pom").expectStatus().isCreated();

        // WHEN
        downloadExchange("/bla/foo/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isOk().expectBody().xml(EXPECTED_XML);
    }

    @Test
    void testArtifactIsImmutable() {
        // GIVEN
        uploadExchange("/bla/foo/1.0.1/foo-1.0.1.pom").expectStatus().isCreated();

        // WHEN
        uploadExchange("/bla/foo/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isForbidden();
    }

    @Test
    void testArtifactPathInvalid() {
        // GIVEN

        // WHEN
        uploadExchange("/bla/foo/1.0.1/bla-1.0.1.pom")

                // THEN
                .expectStatus().isBadRequest();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".md5", ".sha1", ".asc"})
    void testMetadataIsMutable(String extension) {
        // GIVEN
        uploadExchange("/bla/foo/maven-metadata.xml" + extension).expectStatus().isCreated();

        // WHEN
        uploadExchange("/bla/foo/maven-metadata.xml" + extension)

                // THEN
                .expectStatus().isCreated();
    }

    WebTestClient.ResponseSpec downloadExchange(String path) {
        return downloadExchange("releases", path);
    }

    WebTestClient.ResponseSpec downloadExchange(String repository, String path) {
        return webTestClient.get()
                .uri("/maven/" + repository + path)
                .header("authorization", credentials())
                .exchange();
    }

    WebTestClient.ResponseSpec uploadExchange(String path) {
        return uploadExchange("releases", path);
    }

    WebTestClient.ResponseSpec uploadExchange(String repository, String path) {
        return webTestClient.put()
                .uri("/maven/" + repository + path)
                .header("authorization", credentials())
                .header("content-type", "application/xml")
                .contentLength(11)
                .bodyValue(EXPECTED_XML)
                .exchange();
    }

    String credentials() {
        return "Basic " + encodeToString((minio.accessKey() + ":" + minio.secretKey()).getBytes(UTF_8));
    }
}
