package com.tchalanet.server.platform.identity.api;

public final class IdentityProviderException extends RuntimeException {

  private final String code;

  public IdentityProviderException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}

