package com.github.j3t.mvnio.maven.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class MetadataPathValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"/a/b/maven-metadata.xml", "/a/b/1-SNAPSHOT/maven-metadata.xml",
            "/a/b/maven-metadata.xml.md5", "/a/b/maven-metadata.xml.sha1", "/a/b/maven-metadata.xml.asc"})
    void testValidMetadataPath(String path) {
        StepVerifier
                .create(new MetadataPathValidator(path).validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/maven-metadata.xml", "/a/maven-metadata.xml", "/a/b/maven-megadata.xml",
            "/bla/foo/1.0.1/bla-1.0.1.pom"

            /* the following paths are probably invalid but without more context (e.g. content check) we don't know
            "/a/b/1/maven-metadata.xml", "/a/b/1/maven-metadata.xml.md5",
            "/a/b/1/maven-metadata.xml.sha1", "/a/b/1/maven-metadata.xml.asc"
            */
    })
    void testInvalidMetadataPath(String path) {
        StepVerifier
                .create(new MetadataPathValidator(path).validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

}