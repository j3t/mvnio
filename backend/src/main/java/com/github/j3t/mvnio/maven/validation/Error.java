package com.github.j3t.mvnio.maven.validation;

import lombok.Builder;
import lombok.Data;

/**
 * Holds information about a particular validation error.
 */
@Data
@Builder
public class Error {
    private String value;
    private String message;
}
