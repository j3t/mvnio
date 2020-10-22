package com.github.j3t.mvnio.error;

import lombok.NonNull;

public class NotAuthorizedException extends RuntimeException {
    private final String repository;

    public NotAuthorizedException(@NonNull String repository) {
        super("Not authorized, access to repository (" + repository + ") denied");
        this.repository = repository;
    }

    public String getRepository() {
        return repository;
    }
}
