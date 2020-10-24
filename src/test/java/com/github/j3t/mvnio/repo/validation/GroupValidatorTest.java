package com.github.j3t.mvnio.repo.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.test.StepVerifier;

class GroupValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"a", "1", "1a", "1-a2-2/2", "bla/foo"})
    void testValidArtifactIds(String id) {
        StepVerifier
                .create(new GroupValidator(id.split("/")).validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "/", ".1", "-1", "1.", "1-"})
    void testInvalidArtifactIds(String id) {
        StepVerifier
                .create(new GroupValidator(id.split("/")).validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

}