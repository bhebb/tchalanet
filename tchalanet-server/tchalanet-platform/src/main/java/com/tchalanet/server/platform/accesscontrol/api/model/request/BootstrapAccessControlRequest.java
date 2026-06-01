package com.tchalanet.server.platform.accesscontrol.api.model.request;

public record BootstrapAccessControlRequest(BootstrapMode mode) {
  public enum BootstrapMode {
    VALIDATE,
    APPLY_MISSING,
    SYNC_SYSTEM
  }
}
