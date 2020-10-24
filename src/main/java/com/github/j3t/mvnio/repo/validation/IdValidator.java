package com.github.j3t.mvnio.repo.validation;

import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Validates IDs of a given Maven artifact. This basically the artifactId but it also used to validate others like
 * groupIds.
 */
public class IdValidator implements Validator {
    static final String STRING = "[\\w\\d]{1,20}";
    static final String ID = format("^(%1$s[\\.-]){0,20}%1$s$", STRING);
    private static final Pattern ID_PATTERN = Pattern.compile(ID);

    private final String id;

    public IdValidator(String id) {
        this.id = id;
    }

    @Override
    public Mono<Error> validate() {
        return Mono.just(id)
                .filter(id -> !ID_PATTERN.matcher(id).matches())
                .map(e -> Error.builder().value(id).message("Id not valid!").build());
    }
}
