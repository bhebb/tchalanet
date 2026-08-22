package com.tchalanet.server.platform.clientdiagnostics.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticDebugSessionView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventDetailView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticEventView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import com.tchalanet.server.platform.clientdiagnostics.api.model.DeleteClientDiagnosticEventsRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.DeleteClientDiagnosticEventsResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/client-diagnostics")
@RequiredArgsConstructor
@Tag(name = "Platform ops • Client diagnostics")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformClientDiagnosticsController {

  private final ClientDiagnosticsApi clientDiagnosticsApi;

  @GetMapping("/policy")
  public ApiResponse<ClientDiagnosticPolicyView> policy(
      @RequestParam UUID tenantId, @RequestParam UUID sellerTerminalId) {
    return ApiResponse.success(
        clientDiagnosticsApi.getPolicy(
            TenantId.of(tenantId), SellerTerminalId.of(sellerTerminalId)));
  }

  @PutMapping("/policy")
  public ApiResponse<ClientDiagnosticPolicyView> enablePolicy(
      @CurrentContext TchRequestContext ctx,
      @RequestParam UUID tenantId,
      @RequestParam UUID sellerTerminalId,
      @Valid @RequestBody ClientDiagnosticPolicyRequest request) {
    return ApiResponse.success(
        clientDiagnosticsApi.enablePolicy(
            TenantId.of(tenantId),
            SellerTerminalId.of(sellerTerminalId),
            request,
            ctx.currentUserIdRequired().value()));
  }

  @PostMapping("/policy:disable")
  public ApiResponse<ClientDiagnosticPolicyView> disablePolicy(
      @CurrentContext TchRequestContext ctx,
      @RequestParam UUID tenantId,
      @RequestParam UUID sellerTerminalId) {
    return ApiResponse.success(
        clientDiagnosticsApi.disablePolicy(
            TenantId.of(tenantId),
            SellerTerminalId.of(sellerTerminalId),
            ctx.currentUserIdRequired().value()));
  }

  @GetMapping("/events")
  public ApiResponse<List<ClientDiagnosticEventView>> recentEvents(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) UUID sellerTerminalId,
      @RequestParam(defaultValue = "100") int limit) {
    return ApiResponse.success(
        clientDiagnosticsApi.recentEvents(
            tenantId == null ? null : TenantId.of(tenantId),
            sellerTerminalId == null ? null : SellerTerminalId.of(sellerTerminalId),
            limit));
  }

  @GetMapping("/debug-sessions")
  public ApiResponse<List<ClientDiagnosticDebugSessionView>> activeDebugSessions() {
    return ApiResponse.success(clientDiagnosticsApi.activeDebugSessions());
  }

  @GetMapping("/events/{eventId}")
  public ApiResponse<ClientDiagnosticEventDetailView> eventDetail(@PathVariable UUID eventId) {
    return ApiResponse.success(clientDiagnosticsApi.eventDetail(eventId));
  }

  @PostMapping("/events:delete")
  public ApiResponse<DeleteClientDiagnosticEventsResult> deleteEvents(
      @Valid @RequestBody DeleteClientDiagnosticEventsRequest request) {
    return ApiResponse.success(clientDiagnosticsApi.deleteEvents(request.eventIds()));
  }
}
