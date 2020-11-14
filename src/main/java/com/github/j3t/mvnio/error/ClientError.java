package com.github.j3t.mvnio.error;

public class ClientError extends RuntimeException {
    private final int returnCode;

    public ClientError(int returnCode, String message) {
        super(message);
        this.returnCode = returnCode;
    }

    public int getReturnCode() {
        return returnCode;
    }
}
