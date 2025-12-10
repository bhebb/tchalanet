package com.tchalanet.server.features.private_dashboard;

import java.util.List;
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

import com.tchalanet.server.features.pagemodel.shared.PageModelService;
import com.tchalanet.server.features.pagemodel.shared.PageModelTypeResolver;
import com.tchalanet.server.features.pagemodel.shared.PageModel;
import com.tchalanet.server.features.private_dashboard.dynamic.PrivateDashboardDynamicDataService;
import com.tchalanet.server.core.accesscontrol.domain.model.TchRole;
import com.tchalanet.server.features.private_dashboard.block.PrivateDashboardDynamicPayload;
import com.tchalanet.server.features.pagemodel.shared.LangResolver;

@RestController
@RequestMapping("/api/private/dashboard")
@RequiredArgsConstructor
public class PrivateDashboardController {

    private final PrivateDashboardService service;
    private final PrivateDashboardDynamicDataService dynamicDataService;
    private final PageModelService pageModelService;
    private final PageModelTypeResolver pageModelTypeResolver;
    private final LangResolver langResolver;

    @GetMapping
    public ResponseEntity<PrivateDashboardResponse> getDashboard(
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId,
        @RequestHeader(value = "X-User-Lang", required = false) String userPreferredLang
    ) {
        UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();

        PrivateDashboardResponse response = service.getDashboard(
            Optional.ofNullable(lang),
            effectiveUserId,
            userPreferredLang
        );
        return ResponseEntity.ok(response);
    }

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<PrivateDashboardDynamicPayload> getTenantDashboardForSuperadmin(
        @PathVariable UUID tenantId,
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        UUID effectiveUserId = userId != null ? userId : UUID.randomUUID();
        var type = pageModelTypeResolver.forDashboard(TchRole.TENANT_ADMIN);
        PageModel pageModel = pageModelService.loadEffectiveModel(tenantId, type.logicalId());

        String resolvedLang = langResolver.resolve(new LangResolver.LangResolverContext(Optional.ofNullable(lang), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

        PrivateDashboardDynamicPayload payload = dynamicDataService.buildDynamicData(tenantId, effectiveUserId, TchRole.TENANT_ADMIN, resolvedLang, pageModel);
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/tenant/{tenantId}/cashier/{cashierId}")
    public ResponseEntity<PrivateDashboardDynamicPayload> getCashierDashboardForSuperadmin(
        @PathVariable UUID tenantId,
        @PathVariable UUID cashierId,
        @RequestParam(name = "lang", required = false) String lang,
        @RequestHeader(value = "X-User-Id", required = false) UUID userId
    ) {
        UUID effectiveUserId = userId != null ? userId : cashierId;
        var type = pageModelTypeResolver.forDashboard(TchRole.CASHIER);
        PageModel pageModel = pageModelService.loadEffectiveModel(tenantId, type.logicalId());

        String resolvedLang = langResolver.resolve(new LangResolver.LangResolverContext(Optional.ofNullable(lang), Optional.empty(), Optional.empty(), Optional.empty(), List.of(), "fr"));

        PrivateDashboardDynamicPayload payload = dynamicDataService.buildDynamicData(tenantId, effectiveUserId, TchRole.CASHIER, resolvedLang, pageModel);
        return ResponseEntity.ok(payload);
    }
}
