package com.github.j3t.mvnio.maven.validation;

import reactor.core.publisher.Mono;

import static com.github.j3t.mvnio.maven.validation.IdValidator.WORD_REGEX;
import static java.lang.String.format;

/**
 * Validates the filename of a given Maven artifact.
 */
public class ArtifactFilenameValidator implements Validator {
    private final String version;
    private final String filename;
    private final String artifactId;

    /**
     * @param filename the artifacts filename (e.g. b-1.jar, foo-1.0.1-SNAPSHOT-sources.jar, ...)
     * @param version the version of the artifact (e.g. 1, 1.0.1-SNAPSHOT, ...)
     * @param artifactId the ID of the artifact (e.g. b, foo, ...)
     */
    public ArtifactFilenameValidator(String filename, String version , String artifactId) {
        this.filename = filename;
        this.version = version;
        this.artifactId = artifactId;
    }

    @Override
    public Mono<Error> validate() {
        return Mono.just(version)
                .filter(v -> !(v.endsWith("-SNAPSHOT") ? isSnapshotVersion() : isReleaseVersion()))
                .map(e -> Error.builder().value(filename).message("Artifact-Name is not valid!").build());
    }

    private boolean isReleaseVersion() {
        return filename.matches(format("^%1$s-%2$s(-%3$s)?(\\.%3$s){1,2}$", artifactId, version, WORD_REGEX));
    }

    private boolean isSnapshotVersion() {
        return filename.matches(format("^%1$s-%2$s-\\d{8}\\.\\d{6}-\\d{1,6}(\\.%3$s){1,2}$",
                artifactId,
                version.substring(0, version.length() - "-SNAPSHOT".length()),
                WORD_REGEX));
    }
}
