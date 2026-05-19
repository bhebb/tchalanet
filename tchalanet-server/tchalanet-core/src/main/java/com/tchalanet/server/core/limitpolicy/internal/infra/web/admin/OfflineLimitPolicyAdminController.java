package com.tchalanet.server.core.limitpolicy.internal.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.limitpolicy.api.command.offline.UpsertOfflineLimitPolicyCommand;
import com.tchalanet.server.core.limitpolicy.api.model.offline.OfflineLimitPolicy;
import com.tchalanet.server.core.limitpolicy.api.query.GetOfflineLimitPolicyQuery;
import com.tchalanet.server.core.limitpolicy.internal.infra.web.admin.model.UpsertOfflineLimitPolicyRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read + upsert the per-tenant offline limit policy. When no row exists, {@code GET}
 * returns the global defaults from {@code tch.limitpolicy.offline.*}.
 */
@RestController
@RequestMapping("/admin/policies/limits/offline")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Limit Policy • Offline • Admin")
@Validated
public class OfflineLimitPolicyAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    public ApiResponse<OfflineLimitPolicy> get(@CurrentContext TchRequestContext ctx) {
        return ApiResponse.success(queryBus.ask(
            new GetOfflineLimitPolicyQuery(ctx.effectiveTenantIdRequired())));
    }

    @PutMapping
    public ApiResponse<OfflineLimitPolicy> upsert(
        @CurrentContext TchRequestContext ctx,
        @Valid @RequestBody UpsertOfflineLimitPolicyRequest body
    ) {
        var saved = commandBus.execute(new UpsertOfflineLimitPolicyCommand(
            ctx.effectiveTenantIdRequired(),
            body.offlineEnabled(),
            body.batchSize(),
            body.validityDuration(),
            body.syncAcceptedExtension(),
            body.maxTicketCount(),
            body.maxTotalAmount()
        ));
        return ApiResponse.success(saved);
    }
}
