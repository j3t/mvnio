package com.github.j3t.mvnio.repo.validation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
        return groupIdParts.length == 0 ? Mono.just(Error.builder()
                .value(groupIdParts)
                .message("GroupId is empty!")
                .build()) : Flux.just(groupIdParts)
                .flatMap(part -> new IdValidator(part).validate())
                .limitRequest(1)
                .next()
                .map(e -> Error.builder().value(groupIdParts).message("GroupId invalid!").build());
    }
}
