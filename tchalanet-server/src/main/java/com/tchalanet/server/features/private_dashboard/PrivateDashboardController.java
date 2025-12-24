package com.tchalanet.server.features.private_dashboard;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.features.private_dashboard.dynamic.PrivateDashboardDynamicDataService;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;

@RestController
@RequestMapping("/api/private/dashboard")
@RequiredArgsConstructor
public class PrivateDashboardController {

    private final PrivateDashboardService service;
    private final PrivateDashboardDynamicDataService dynamicDataService;

    @GetMapping
    public ResponseEntity<ApiResponse<PrivateDashboardResponse>> getDashboard(
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId,
        @RequestHeader(value = "X-User-Lang", required = false) String userPreferredLang
    ) {
        UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();

        ApiResponse<PrivateDashboardResponse> response = service.getDashboard(
            Optional.ofNullable(lang),
            effectiveUserId,
            userPreferredLang
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<ApiResponse<PrivateDashboardDynamicPayload>> getTenantDashboardForSuperadmin(
        @PathVariable UUID tenantId,
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();
        ApiResponse<PrivateDashboardDynamicPayload> response = service.getTenantDashboardForSuperadmin(tenantId, Optional.ofNullable(lang), effectiveUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}/cashier/{cashierId}")
    public ResponseEntity<ApiResponse<PrivateDashboardDynamicPayload>> getCashierDashboardForSuperadmin(
        @PathVariable UUID tenantId,
        @PathVariable UUID cashierId,
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        var effectiveUserId = userId != null ? userId : cashierId;
        var response = service.getCashierDashboardForSuperadmin(tenantId, cashierId, Optional.ofNullable(lang), effectiveUserId);
        return ResponseEntity.ok(response);
    }
}
