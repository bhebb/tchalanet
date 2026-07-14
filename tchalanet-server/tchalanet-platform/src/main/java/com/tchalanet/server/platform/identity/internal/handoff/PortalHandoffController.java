package com.tchalanet.server.platform.identity.internal.handoff;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.ConsumePortalHandoffRequest;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.ConsumePortalHandoffResponse;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.CreatePortalHandoffRequest;
import com.tchalanet.server.platform.identity.internal.handoff.PortalHandoffModels.CreatePortalHandoffResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/auth/portal-handoffs")
@RequiredArgsConstructor
class PortalHandoffController {

  private final PortalHandoffService service;
  private final PortalHandoffRateLimiter rateLimiter;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a one-time cross-origin portal auth handoff")
  ApiResponse<CreatePortalHandoffResponse> create(
      @Valid @RequestBody CreatePortalHandoffRequest request) {
    return ApiResponse.success(
        service.create(
            request.targetPortal(), request.entryRoute(), request.supportAccessSessionId()));
  }

  @PostMapping("/{handoffId}/consume")
  @Operation(summary = "Consume a one-time cross-origin portal auth handoff")
  ApiResponse<ConsumePortalHandoffResponse> consume(
      @PathVariable UUID handoffId,
      @Valid @RequestBody ConsumePortalHandoffRequest request,
      HttpServletRequest servletRequest) {
    rateLimiter.check(clientIp(servletRequest), handoffId);
    return ApiResponse.success(service.consume(handoffId, request.code(), request.targetPortal()));
  }

  private static String clientIp(HttpServletRequest request) {
    var forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
