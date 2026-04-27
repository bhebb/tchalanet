package com.tchalanet.server.common.error;

import jakarta.persistence.EntityNotFoundException;

/**
 * Generic not-found exception used across the application. Extends {@link EntityNotFoundException}
 * so existing error handling that maps that exception to 404 / ProblemDetail continues to work.
 */
public class NotFoundException extends EntityNotFoundException {

  public NotFoundException(String message) {
    super(message);
  }

  public NotFoundException(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
