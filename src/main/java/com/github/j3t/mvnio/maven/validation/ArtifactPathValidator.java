package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Arrays;

/**
 * Validates the repository path of a given Maven artifact.
 */
public class ArtifactPathValidator implements Validator {
    private final String path;

    /**
     * @param path the path of the artifact relative to the repository (e.g. /bla/foo/foo-1.0.1.jar)
     */
    public ArtifactPathValidator(String path) {
        this.path = path;
    }

    @Override
    public Mono<Error> validate() {
        String[] parts = path.split("/", 30);

        String fileName = parts[parts.length - 1];
        String version = parts[parts.length - 2];
        String artifactId = parts[parts.length - 3];
        String[] groupIdParts = Arrays.copyOfRange(parts, 1, parts.length - 3);

        return Flux.just(
                new GroupValidator(groupIdParts),
                new IdValidator(artifactId),
                new VersionValidator(version),
                new ArtifactFilenameValidator(fileName, version, artifactId))
                .flatMap(Validator::validate)
                .limitRequest(1)
                .next()
                .map(e -> Error.builder().value(path).message("Artifact-Path is not valid!").build());
    }
}
