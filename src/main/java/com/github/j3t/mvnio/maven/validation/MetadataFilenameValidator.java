package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Mono;

import static com.github.j3t.mvnio.maven.validation.IdValidator.WORD_REGEX;
import static java.lang.String.format;

/**
 * Validates the metadata filename of a given Maven artifact.
 */
public class MetadataFilenameValidator implements Validator {
    private final String filename;

    public MetadataFilenameValidator(String filename) {
        this.filename = filename;
    }

    @Override
    public Mono<Error> validate() {
        return Mono.just(filename)
                .filter(f -> !f.matches(format("^maven-metadata\\.xml(\\.%s)?$", WORD_REGEX)))
                .map(e -> Error.builder().value(filename).message("Not a valid metadata filename!").build());
    }
}
