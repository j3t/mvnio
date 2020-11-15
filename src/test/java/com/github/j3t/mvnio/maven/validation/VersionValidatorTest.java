package com.github.j3t.mvnio.maven.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import reactor.test.StepVerifier;

class VersionValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"2.2", "1", "4.1.2.RELEASE", "4.1.2-SNAPSHOT"})
    void testValidVersions(String version) {
        StepVerifier
                .create(new VersionValidator(version).validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", ".", "-", ".1", "-1", "1.", "1-"})
    void testInvalidVersions(String version) {
        StepVerifier
                .create(new VersionValidator(version).validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }

}