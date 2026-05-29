package com.tchalanet.server.platform.entitlement.internal;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.entitlement.api.EntitlementApi;
import com.tchalanet.server.platform.entitlement.api.model.TenantCapabilitySnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tenant/me/capabilities")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TenantCapabilityController {

    private final EntitlementApi entitlementApi;

    @GetMapping
    public ApiResponse<TenantCapabilitySnapshot> getMyCapabilities(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(entitlementApi.getSnapshot(ctx.effectiveTenantIdRequired()));
    }
}
