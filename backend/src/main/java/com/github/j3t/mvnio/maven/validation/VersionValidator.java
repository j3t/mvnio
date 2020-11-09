package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

import static com.github.j3t.mvnio.maven.validation.IdValidator.ID_REGEX;
import static java.lang.String.format;

/**
 * Validates the version of a given Maven artifact.
 */
public class VersionValidator implements Validator {
    static final Pattern VERSION_PATTERN = Pattern.compile(format("^%1$s(-SNAPSHOT)?$", ID_REGEX));

    private final String version;

    /**
     * @param version the version of the artifact (e.g. 1, 1.0.1-SNAPSHOT, ...)
     */
    public VersionValidator(String version) {
        this.version = version;
    }

    @Override
    public Mono<Error> validate() {
        return Mono.just(version)
                .filter(v -> !VERSION_PATTERN.matcher(v).matches())
                .map(v -> Error.builder().value(v).message("Version not valid!").build());
    }
}
