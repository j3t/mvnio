package com.github.j3t.mvnio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import testcontainers.MinioContainer;
import testcontainers.MinioMcContainer;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

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
    ApplicationContext context;

    WebTestClient webTestClient;

    @DynamicPropertySource
    static void appProperties(DynamicPropertyRegistry registry) {
        registry.add("s3.override-endpoint", () -> true);
        registry.add("s3.endpoint", () -> minio.getExternalAddress());
    }

    @BeforeEach
    void initTestBucket() throws Exception {
        this.webTestClient = WebTestClient.bindToApplicationContext(this.context)
                .configureClient()
                .filter(basicAuthentication(minio.accessKey(), minio.secretKey()))
                .build();
        mc.deleteBucket("releases");
        mc.createBucket("releases");
    }

    @Test
    void testAuthenticateChallenge() {
        // GIVEN

        // WHEN
        WebTestClient.bindToApplicationContext(this.context)
                .configureClient()
                .build()
                .get()
                .uri("/maven/releases")
                .exchange()

                // THEN
                .expectStatus().isUnauthorized()
                .expectHeader().value("WWW-Authenticate", is("Basic realm=\"s3\", bucket=\"releases\""));
    }

    @Test
    void testUploadRepositoryNotFound() {
        // GIVEN

        // WHEN
        uploadExchange(UUID.randomUUID().toString(), "/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testUploadSnapshotRepositoryNotFound() {
        // GIVEN

        // WHEN
        uploadExchange("snapshots", "/foo/bar/1.0.1-SNAPSHOT/bar-1.0.1-20201023.142512-1.jar")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testDownloadRepositoryNotFound() {
        // GIVEN

        // WHEN
        downloadExchange(UUID.randomUUID().toString(), "/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testArtifactNotFound() {
        // GIVEN

        // WHEN
        downloadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound();
    }

    @Test
    void testArtifactIsUploadedAndAvailable() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")
                .expectStatus().isCreated();

        // WHEN
        downloadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isOk().expectBody().xml(EXPECTED_XML);
    }

    @Test
    void testArtifactIsImmutable() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom").expectStatus().isCreated();

        // WHEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isForbidden();
    }

    @Test
    void testArtifactPathInvalid() {
        // GIVEN

        // WHEN
        uploadExchange("/foo/bar/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isBadRequest();
    }

    @Test
    void testMetadataIsEmpty() {
        // GIVEN

        // WHEN
        metadata()

                // THEN
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void testMetadata() {
        // GIVEN
        uploadExchange("/foo/bar/maven-metadata.xml").expectStatus().isCreated();

        // WHEN
        metadata()

                // THEN
                .expectStatus().isOk()
                .expectBody().json("[\"/foo/bar/maven-metadata.xml\"]");
    }

    @Test
    void testList() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom").expectStatus().isCreated();
        uploadExchange("/foo/bar/1.0.2/bar-1.0.2.pom").expectStatus().isCreated();
        uploadExchange("/foo/bar/1.0.3/bar-1.0.3.pom").expectStatus().isCreated();
        uploadExchange("/foo/bar/maven-metadata.xml").expectStatus().isCreated();

        // WHEN
        list("/foo/bar")

                // THEN
                .expectStatus().isOk()
                .expectBody().json("[\"1.0.1/\",\"1.0.2/\",\"1.0.3/\",\"maven-metadata.xml\"]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".md5", ".sha1", ".asc"})
    void testMetadataIsMutable(String extension) {
        // GIVEN
        uploadExchange("/foo/bar/maven-metadata.xml" + extension).expectStatus().isCreated();

        // WHEN
        uploadExchange("/foo/bar/maven-metadata.xml" + extension)

                // THEN
                .expectStatus().isCreated();
    }

    private WebTestClient.ResponseSpec list(String path) {
        return webTestClient.get()
                .uri("/list/releases"+path)
                .exchange();
    }

    private WebTestClient.ResponseSpec metadata() {
        return webTestClient.get()
                .uri("/metadata/releases")
                .exchange();
    }

    WebTestClient.ResponseSpec downloadExchange(String path) {
        return downloadExchange("releases", path);
    }

    WebTestClient.ResponseSpec downloadExchange(String repository, String path) {
        return webTestClient.get()
                .uri("/maven/" + repository + path)
                .exchange();
    }

    WebTestClient.ResponseSpec uploadExchange(String path) {
        return uploadExchange("releases", path);
    }

    WebTestClient.ResponseSpec uploadExchange(String repository, String path) {
        return webTestClient.put()
                .uri("/maven/" + repository + path)
                .header("content-type", "application/xml")
                .contentLength(11)
                .bodyValue(EXPECTED_XML)
                .exchange();
    }
}
