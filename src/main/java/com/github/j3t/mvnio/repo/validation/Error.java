package com.github.j3t.mvnio.repo.validation;

import lombok.Builder;
import lombok.Data;

/**
 * Holds information about a particular validation error.
 */
@Data
@Builder
public class Error {
    private Object value;
    private String message;
}
