package com.github.j3t.mvnio.maven.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class IdValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "1", "1a", "1-a2-2.2", "linux-x86_64", "_"})
    void testValidArtifactIds(String id) {
        StepVerifier
                .create(new IdValidator(id).validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".", "-", ".1", "-1", "1.", "1-"})
    void testInvalidArtifactIds(String id) {
        StepVerifier
                .create(new IdValidator(id).validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

}