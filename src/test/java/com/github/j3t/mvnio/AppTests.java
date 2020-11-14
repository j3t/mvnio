package com.github.j3t.mvnio;

import static org.hamcrest.Matchers.is;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;
import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.http.HttpDocumentation;
import org.springframework.restdocs.templates.TemplateFormats;
import org.springframework.restdocs.webtestclient.WebTestClientSnippetConfigurer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import testcontainers.MinioContainer;
import testcontainers.MinioMcContainer;

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
    @RegisterExtension
    final RestDocumentationExtension restDocumentation = new RestDocumentationExtension("docs/app-tests");
    @Autowired
    ApplicationContext context;

    WebTestClient webTestClient;

    @DynamicPropertySource
    static void appProperties(DynamicPropertyRegistry registry) {
        registry.add("s3.override-endpoint", () -> true);
        registry.add("s3.endpoint", () -> minio.getExternalAddress());
    }

    @BeforeEach
    void initTestBucket(RestDocumentationContextProvider restDocumentation) throws Exception {
        this.webTestClient = createWebClient(restDocsFilter(restDocumentation), basicAuthentication(minio.accessKey(), minio.secretKey()));
        mc.deleteBucket("releases");
        mc.createBucket("releases");
    }

    @Test
    void testAuthenticateChallenge(RestDocumentationContextProvider restDocumentation) {
        // GIVEN

        // WHEN
        createWebClient(restDocsFilter(restDocumentation))
                .get()
                .uri("/maven/releases")
                .exchange()

                // THEN
                .expectStatus().isUnauthorized()
                .expectHeader().value("WWW-Authenticate", is("Basic realm=\"s3\", bucket=\"releases\""))
                .expectBody().consumeWith(document("unauthorized"));
    }

    @Test
    void testUploadRepositoryNotFound() {
        // GIVEN

        // WHEN
        uploadExchange("third-party", "/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isNotFound()
                .expectBody().consumeWith(document("repositoryNotExists"));
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
                .expectStatus().isNotFound()
                .expectBody().consumeWith(document("artifactNotExists"));
    }

    @Test
    void testArtifactIsUploadedAndAvailable() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")
                .expectStatus().isCreated()
                .expectBody().consumeWith(document("upload"));

        // WHEN
        downloadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isOk().expectBody().xml(EXPECTED_XML).consumeWith(document("download"));
    }

    @Test
    void testArtifactIsImmutable() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom").expectStatus().isCreated();

        // WHEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom")

                // THEN
                .expectStatus().isForbidden()
                .expectBody().consumeWith(document("artifactAlreadyExists"));
    }

    @Test
    void testArtifactPathInvalid() {
        // GIVEN

        // WHEN
        uploadExchange("/foo/bar/1.0.1/foo-1.0.1.pom")

                // THEN
                .expectStatus().isBadRequest()
                .expectBody().consumeWith(document("artifactPathNotValid"));
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
                .expectBody().json("[\"/foo/bar/maven-metadata.xml\"]").consumeWith(document("metadata"));
    }

    @Test
    void testList() {
        // GIVEN
        uploadExchange("/foo/bar/1.0.1/bar-1.0.1.pom").expectStatus().isCreated();
        uploadExchange("/foo/bar/maven-metadata.xml").expectStatus().isCreated();

        // WHEN
        list("/foo/bar")

                // THEN
                .expectStatus().isOk()
                .expectBody().json("[\"1.0.1/\",\"maven-metadata.xml\"]").consumeWith(document("list"));
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

    private WebTestClient createWebClient(ExchangeFilterFunction...filters) {
        return WebTestClient.bindToApplicationContext(this.context)
                .configureClient()
                .filters(exchangeFilterFunctions -> exchangeFilterFunctions.addAll(Arrays.asList(filters)))
                .build();
    }

    private WebTestClient.ResponseSpec list(String path) {
        return webTestClient.get()
                .uri("/list/releases" + path)
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

    private WebTestClientSnippetConfigurer restDocsFilter(RestDocumentationContextProvider restDocumentation) {
        return documentationConfiguration(restDocumentation)
                .snippets()
                .withDefaults(HttpDocumentation.httpRequest(), HttpDocumentation.httpResponse())
                .withTemplateFormat(TemplateFormats.markdown());
    }
}
