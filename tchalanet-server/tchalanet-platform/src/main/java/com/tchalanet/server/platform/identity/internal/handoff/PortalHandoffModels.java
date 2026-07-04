package com.tchalanet.server.platform.identity.internal.handoff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public final class PortalHandoffModels {
  private PortalHandoffModels() {}

  public record CreatePortalHandoffRequest(
      @NotNull PortalTarget targetPortal,
      @NotBlank String entryRoute,
      UUID supportAccessSessionId
  ) {}

  public record CreatePortalHandoffResponse(
      UUID handoffId,
      String code,
      PortalTarget targetPortal,
      String targetUrl,
      String entryRoute,
      Instant expiresAt
  ) {}

  public record ConsumePortalHandoffRequest(
      @NotBlank String code,
      @NotNull PortalTarget targetPortal
  ) {}

  public record ConsumePortalHandoffResponse(
      String customToken,
      PortalTarget targetPortal,
      String entryRoute,
      UUID supportAccessSessionId
  ) {}
}
