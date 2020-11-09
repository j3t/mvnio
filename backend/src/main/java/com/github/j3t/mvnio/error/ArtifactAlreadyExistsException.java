package com.github.j3t.mvnio.error;

public class ArtifactAlreadyExistsException extends RuntimeException {
    public ArtifactAlreadyExistsException() {
        super("Artifact override is not allowed");
    }
}
