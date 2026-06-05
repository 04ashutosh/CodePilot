package com.codepilot.repository.exception;

/**
 * Thrown when a Git operation (clone, pull, checkout) fails.
 */
public class RepositoryOperationException extends RuntimeException {
    public RepositoryOperationException(String message) {
        super(message);
    }

    public RepositoryOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}