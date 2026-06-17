package com.tchalanet.server.core.session.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.types.id.SalesSessionId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.session.api.query.FinalizeSalesSessionCommand;
import com.tchalanet.server.core.session.internal.infra.web.model.FinalizeSalesSessionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/sales-sessions/{sessionId}/operational-controls")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class AdminSalesSessionOperationalControlsController {

    private final CommandBus commandBus;

    @PatchMapping("/finalize")
    public ApiResponse<Void> finalizeSession(
        @CurrentContext TchRequestContext ctx,
        @PathVariable SalesSessionId sessionId,
        @Valid @RequestBody FinalizeSalesSessionRequest request) {

        commandBus.execute(new FinalizeSalesSessionCommand(
            ctx.tenantId(),
            sessionId,
            request.reason(),
            ctx.currentUserIdRequired()));
        return ApiResponse.success(null);
    }
}
