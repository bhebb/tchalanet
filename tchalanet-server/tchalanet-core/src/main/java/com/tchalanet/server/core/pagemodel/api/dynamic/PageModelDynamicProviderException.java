package com.tchalanet.server.core.pagemodel.api.dynamic;

public class PageModelDynamicProviderException extends RuntimeException {

  private final String code;

  public PageModelDynamicProviderException(String code, String message) {
    super(message);
    this.code = code;
  }

  public PageModelDynamicProviderException(String code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public String code() {
    return code;
  }
}
