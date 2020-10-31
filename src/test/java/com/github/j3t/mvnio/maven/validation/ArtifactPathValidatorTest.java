package com.github.j3t.mvnio.maven.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class ArtifactPathValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"/a/b/1/b-1.jar", "/a/b/1/b-1.jar.md5",
            "/a/b/1/b-1.jar.sha1", "/a/b/1/b-1.jar.asc", "/a/b/1/b-1-sources.jar", "/a/b/1/b-1-javadoc.jar",
            "/a/b/1/b-1-jdk11.jar",
            "/foo/bar/1.0.2-SNAPSHOT/bar-1.0.2-20201023.142512-1.jar"
    })
    void testValidArtifactPath(String path) {
        StepVerifier
                .create(new ArtifactPathValidator(path).validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/a/1/a-1.jar", "/a/b/1/b-2.jar", "/a/b/1-SNAPSHOT/b-1-SNAPSHOT.jar",})
    void testInvalidArtifactPath(String path) {
        StepVerifier
                .create(new ArtifactPathValidator(path).validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

}