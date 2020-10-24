package com.github.j3t.mvnio.repo.validation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Validates the repository path of a given Maven artifact' metadata.
 */
public class MetadataPathValidator implements Validator {
    private final String path;

    /**
     * @param path the path of the metadata file relative to the repository (e.g. /bla/foo/maven-metadata.xml)
     */
    public MetadataPathValidator(String path) {
        this.path = path;
    }

    @Override
    public Mono<Error> validate() {
        String[] parts = path.split("/", 20);

        if (parts.length < 4)
            return Mono.just(Error.builder().value(path).message("Not a valid metadata-path!").build());

        String fileName = parts[parts.length - 1];
        String versionOrArtifactId = parts[parts.length - 2];
        String artifactIdOrGroupIdPart = parts[parts.length - 3];

        String[] groups = Arrays.copyOfRange(parts, 1, parts.length - 2);

        return Flux.just(
                new GroupValidator(groups),
                new IdValidator(artifactIdOrGroupIdPart),
                new VersionValidator(versionOrArtifactId),
                new MetadataFilenameValidator(fileName))
                .flatMap(Validator::validate)
                .limitRequest(1)
                .next().map(e -> Error.builder().value(path).message("Not a valid metadata-path!").build());
    }
}
