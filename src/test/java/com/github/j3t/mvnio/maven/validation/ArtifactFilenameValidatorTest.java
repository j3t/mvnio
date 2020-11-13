package com.github.j3t.mvnio.maven.validation;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

class ArtifactFilenameValidatorTest {

    private static Stream<Arguments> validValidators() {
        return Stream.of(
                Arguments.of(new ArtifactFilenameValidator("b-1.jar", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1.jar.md5", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1.jar.sha1", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1.jar.asc", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1-sources.jar", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1-javadoc.jar", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1-jdk11.jar", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1.0.2-20201023.142512-1.jar", "1.0.2-SNAPSHOT", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-4.1.53-20201112.210114-1-linux-x86_64.jar", "4.1.53-SNAPSHOT","b"))
        );
    }

    private static Stream<Arguments> invalidValidators() {
        return Stream.of(
                Arguments.of(new ArtifactFilenameValidator("b-2.jar", "1", "b")),
                Arguments.of(new ArtifactFilenameValidator("b-1-SNAPSHOT.jar", "1-SNAPSHOT", "b"))
        );
    }

    @ParameterizedTest
    @MethodSource("validValidators")
    void testValidArtifactPath(ArtifactFilenameValidator validator) {
        StepVerifier
                .create(validator.validate())
                .expectComplete()
                .verify();
    }

    @ParameterizedTest
    @MethodSource("invalidValidators")
    void testInvalidArtifactName(ArtifactFilenameValidator validator) {
        StepVerifier
                .create(validator.validate())
                .expectNextCount(1)
                .expectComplete()
                .verify();
    }


}