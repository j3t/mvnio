package com.github.j3t.mvnio.error;

public class NotAuthorizedException extends RuntimeException {

    public NotAuthorizedException() {
        super("Not authorized, access denied");
    }

}
