package com.github.j3t.mvnio.error;

public class ArtifactPathNotValidException extends RuntimeException {
    public ArtifactPathNotValidException() {
        super("Path is not a valid Maven repository path");
    }
}
