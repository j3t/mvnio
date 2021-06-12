package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Validates a given Maven groupId.
 */
public class GroupValidator implements Validator {
    private final String[] groupIdParts;

    /**
     * @param groupIdParts the whole groupId but in parts split by '.' (e.g. a.b.c -> [a,b,c])
     */
    public GroupValidator(String[] groupIdParts) {
        this.groupIdParts = groupIdParts;
    }

    @Override
    public Mono<Error> validate() {
        return groupIdParts.length == 0 ? emptyPath() : notEmptyPath();
    }

    private Mono<Error> notEmptyPath() {
        return Flux.just(groupIdParts)
                .flatMap(part -> new IdValidator(part).validate())
                .take(1, true)
                .next()
                .map(e -> Error.builder().value(Arrays.toString(groupIdParts)).message("GroupId invalid!").build());
    }

    private Mono<Error> emptyPath() {
        return Mono.just(Error.builder()
                .value(Arrays.toString(groupIdParts))
                .message("GroupId empty!")
                .build());
    }
}
