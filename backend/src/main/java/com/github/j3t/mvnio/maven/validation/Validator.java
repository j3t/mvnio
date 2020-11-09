package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Mono;

/**
 * Validator specification. In general, validators are used to validate repository artifacts.
 */
public interface Validator {
    /**
     * Validates the given context and returns an empty mono if it's valid otherwise an {@link Error} is returned.
     */
    Mono<Error> validate();
}
