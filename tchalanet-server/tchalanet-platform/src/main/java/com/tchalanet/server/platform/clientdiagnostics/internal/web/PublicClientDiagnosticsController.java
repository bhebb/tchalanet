package com.tchalanet.server.platform.clientdiagnostics.internal.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.clientdiagnostics.api.ClientDiagnosticsApi;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticIngestionResult;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPublicBatchRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPublicPolicyRequest;
import com.tchalanet.server.platform.clientdiagnostics.api.model.ClientDiagnosticPolicyView;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/public/client-diagnostics")
@RequiredArgsConstructor
@Tag(name = "Public • Client diagnostics")
public class PublicClientDiagnosticsController {

  private final ClientDiagnosticsApi clientDiagnosticsApi;

  @PostMapping("/policy")
  public ResponseEntity<ApiResponse<ClientDiagnosticPolicyView>> policy(
      @Valid @RequestBody ClientDiagnosticPublicPolicyRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(ApiResponse.success(clientDiagnosticsApi.publicPolicy(request)));
  }

  @PostMapping("/events")
  public ResponseEntity<ApiResponse<ClientDiagnosticIngestionResult>> ingest(
      HttpServletRequest httpRequest, @Valid @RequestBody ClientDiagnosticPublicBatchRequest request) {
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header("Pragma", "no-cache")
        .body(
            ApiResponse.success(
                clientDiagnosticsApi.ingestPublic(
                    httpRequest.getHeader("X-Request-Id"), request)));
  }
}
