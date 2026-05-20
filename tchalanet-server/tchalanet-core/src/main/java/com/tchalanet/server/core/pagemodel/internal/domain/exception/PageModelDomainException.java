package com.tchalanet.server.core.pagemodel.internal.domain.exception;

/**
 * Base exception for all pagemodel domain violations.
 */
public class PageModelDomainException extends RuntimeException {

  public PageModelDomainException(String message) {
    super(message);
  }

  public PageModelDomainException(String message, Throwable cause) {
    super(message, cause);
  }
}

