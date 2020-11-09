package com.github.j3t.mvnio.maven;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import reactor.test.StepVerifier;

import java.util.stream.Stream;

class ContentTypeResolverTest {

    private static Stream<Arguments> extensionsAndTypes() {
        return Stream.of(
                Arguments.of(".xml", MediaType.APPLICATION_XML),
                Arguments.of(".pom", MediaType.APPLICATION_XML),

                Arguments.of(".sha1", MediaType.TEXT_PLAIN),
                Arguments.of(".md5", MediaType.TEXT_PLAIN),

                Arguments.of(".asc", MediaType.valueOf("application/pgp-signature")),

                Arguments.of(".jar", MediaType.valueOf("application/java-archive")),
                Arguments.of(".war", MediaType.valueOf("application/java-archive")),
                Arguments.of(".ear", MediaType.valueOf("application/java-archive")),

                Arguments.of(".zip", MediaType.valueOf("application/zip"))
        );
    }

    private static Stream<String> extensionsWithoutTypes() {
        return Stream.of("bla", ".bla", ".unknown", "", null);
    }

    @ParameterizedTest
    @MethodSource("extensionsAndTypes")
    void testExtensionsWithTypes(String path, MediaType mediaType) {
        StepVerifier
                .create(ContentTypeResolver.findByPath(path))
                .expectNext(mediaType)
                .expectComplete()
                .verify();

    }

    @ParameterizedTest
    @MethodSource("extensionsWithoutTypes")
    void testExtensionsWithoutTypes(String path) {
        StepVerifier
                .create(ContentTypeResolver.findByPath(path))
                .expectNextCount(0)
                .expectComplete()
                .verify();
    }
}