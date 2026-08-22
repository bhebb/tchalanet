package com.tchalanet.server.platform.clientdiagnostics.internal.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionContext;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/client-diagnostics")
@RequiredArgsConstructor
@Tag(name = "Tenant • Client diagnostics")
@PreAuthorize(
    "hasAuthority('ACTOR_SELLER_TERMINAL') and hasAuthority('PERM_client_diagnostics.write')")
public class TenantClientDiagnosticsController {

  private final ClientDiagnosticsApi clientDiagnosticsApi;

  @PostMapping("/events")
  public ApiResponse<ClientDiagnosticIngestionResult> ingest(
      @CurrentContext TchRequestContext context,
      @Valid @RequestBody ClientDiagnosticBatchRequest request) {
    return ApiResponse.success(
        clientDiagnosticsApi.ingest(
            new ClientDiagnosticIngestionContext(
                context.tenantIdRequired(),
                context.sellerTerminalIdRequired(),
                context.requestId(),
                Instant.now()),
            request));
  }
}
