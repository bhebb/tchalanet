package com.tchalanet.server.features.tenantadmin.config.identity;

import com.tchalanet.server.catalog.tenant.api.TenantCatalog;
import com.tchalanet.server.catalog.tenant.api.model.TenantRegistryView;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.tenantconfig.application.command.model.UpdateTenantIdentityCommand;
import com.tchalanet.server.features.tenantadmin.config.model.TenantIdentityView;
import com.tchalanet.server.features.tenantadmin.config.model.UpdateTenantIdentityRequest;
import jakarta.validation.Valid;
import java.time.ZoneId;
import java.util.Currency;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/config/identity")
@PreAuthorize("hasAnyRole('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantAdminIdentityController {

    private final CommandBus commandBus;
    private final TenantCatalog tenantCatalog;

    @GetMapping
    public ApiResponse<TenantIdentityView> getIdentity(@CurrentContext TchRequestContext ctx) {
        TenantId tenantId = ctx.tenantIdSafe();
        if (tenantId == null) throw ProblemRest.badRequest("tenant not resolved");

        TenantRegistryView reg =
            tenantCatalog.findRegistryById(tenantId)
                .orElseThrow(() -> ProblemRest.notFound("tenant not found: " + tenantId));

        return ApiResponse.success(toView(reg));
    }

    @PutMapping
    public ApiResponse<TenantIdentityView> updateIdentity(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpdateTenantIdentityRequest req
    ) {
        TenantId tenantId = ctx.tenantIdSafe();
        if (tenantId == null) throw ProblemRest.badRequest("tenant not resolved");

        ZoneId zone = null;
        Currency currency = null;

        if (req.timeZone() != null && !req.timeZone().isBlank()) {
            try {
                zone = ZoneId.of(req.timeZone().trim());
            } catch (Exception e) {
                throw ProblemRest.badRequest("invalid timeZone: " + req.timeZone());
            }
        }

        if (req.currency() != null && !req.currency().isBlank()) {
            try {
                currency = Currency.getInstance(req.currency().trim().toUpperCase());
            } catch (Exception e) {
                throw ProblemRest.badRequest("invalid currency: " + req.currency());
            }
        }

        commandBus.send(new UpdateTenantIdentityCommand(tenantId, req.name(), zone, currency));

        TenantRegistryView reg =
            tenantCatalog.findRegistryById(tenantId)
                .orElseThrow(() -> ProblemRest.notFound("tenant not found: " + tenantId));

        return ApiResponse.success(toView(reg));
    }

    private static TenantIdentityView toView(TenantRegistryView reg) {
        return new TenantIdentityView(
            reg.tenantId().value().toString(),
            reg.code(),
            reg.name(),
            reg.timezone() == null ? null : reg.timezone().toString(),
            reg.currency() == null ? null : reg.currency().getCurrencyCode(),
            reg.status().name(),
            reg.type().name()
        );
    }
}
