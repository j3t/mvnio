package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Validates IDs of a given Maven artifact. This basically the artifactId but it also used to validate others like
 * groupIds.
 */
public class IdValidator implements Validator {
    static final String WORD_REGEX = "[\\w\\d]{1,20}";
    static final String ID_REGEX = format("^(%1$s[\\.-]){0,20}%1$s$", WORD_REGEX);
    private static final Pattern ID_PATTERN = Pattern.compile(ID_REGEX);

    private final String id;

    public IdValidator(String id) {
        this.id = id;
    }

    @Override
    public Mono<Error> validate() {
        return Mono.just(id)
                .filter(v -> !ID_PATTERN.matcher(v).matches())
                .map(v -> Error.builder().value(v).message("Id not valid!").build());
    }
}
