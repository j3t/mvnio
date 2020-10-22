package com.github.j3t.mvnio.error;

import static com.github.j3t.mvnio.repo.RepositoryHelper.ARTIFACT_PATTERN;

public class ArtifactPathNotValidException extends RuntimeException {
    public ArtifactPathNotValidException() {
        super("Path points not to a valid Maven artifact! regex: '" + ARTIFACT_PATTERN.pattern() + "'");
    }
}
