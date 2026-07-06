package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/entitlements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformEntitlementController {

    private final EntitlementApi entitlementApi;

    @GetMapping("/{tenantId}")
    public ApiResponse<TenantCapabilitySnapshot> getSnapshot(@PathVariable TenantId tenantId) {
        return ApiResponse.success(entitlementApi.getSnapshot(tenantId));
    }
}
