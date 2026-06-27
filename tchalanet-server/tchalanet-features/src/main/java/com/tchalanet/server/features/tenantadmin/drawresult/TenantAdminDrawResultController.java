package com.tchalanet.server.features.tenantadmin.drawresult;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultCommand;
import com.tchalanet.server.core.drawresult.api.command.RecordManualDrawResultResult;
import com.tchalanet.server.features.tenantadmin.drawresult.model.ProposeManualDrawResultRequest;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.AuditAction;
import com.tchalanet.server.platform.audit.api.model.AuditEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;

@RestController
@RequestMapping("/admin/draw-results")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN')")
@Tag(name = "Tenant Admin • Draw Results")
public class TenantAdminDrawResultController {

    private final CommandBus commandBus;

    @Operation(summary = "Propose a manual draw result — status determined by slot trust policy")
    @PostMapping("/manual")
    @AuditLog(
        entity = AuditEntityType.DRAW_RESULT,
        action = AuditAction.DRAW_RESULT_PROPOSE,
        idExpression = "#result.data().drawResultId().value().toString()",
        detailsExpression = "#req")
    public ApiResponse<RecordManualDrawResultResult> propose(
        @Valid @RequestBody ProposeManualDrawResultRequest req,
        @CurrentContext TchRequestContext ctx
    ) {
        var res = commandBus.execute(
            new RecordManualDrawResultCommand(
                ctx.tenantId(),
                req.drawDate(),
                req.slotKey().trim().toUpperCase(Locale.ROOT),
                ctx.externalSubject(),
                req.notes(),
                req.pick3(),
                req.pick4(),
                false,   // tenant admin cannot force-overwrite
                null,
                true     // observe trust_policy: REQUIRE_PLATFORM_REVIEW → PROVISIONAL
            )
        );

        return ApiResponse.success(res);
    }
}
