package com.tchalanet.server.core.draw.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.draw.api.command.ArchiveDrawCommand;
import com.tchalanet.server.core.draw.api.command.CancelDrawCommand;
import com.tchalanet.server.core.draw.api.command.DrawLifecycleCommandLimits;
import com.tchalanet.server.core.draw.api.command.LockDrawCommand;
import com.tchalanet.server.core.draw.api.command.SettleDrawCommand;
import com.tchalanet.server.core.draw.api.command.UnlockDrawCommand;
import com.tchalanet.server.core.draw.api.query.GetDrawByIdQuery;
import com.tchalanet.server.core.draw.internal.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.internal.infra.web.model.ArchiveDrawRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.CancelDrawRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.DrawSummaryResponse;
import com.tchalanet.server.core.draw.internal.infra.web.model.LockDrawRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.SettleDrawRequest;
import com.tchalanet.server.core.draw.internal.infra.web.model.UnlockDrawRequest;
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

import java.util.List;

@RestController
@RequestMapping("/admin/draws/lifecycle")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Draws • Lifecycle")
public class DrawLifecycleAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;
    private final DrawAdminWebMapper mapper;

    @Operation(summary = "Cancel multiple draws")
    @PostMapping("/cancel")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_CANCEL,
        idExpression = "#request.drawIds().toString()",
        detailsExpression = "#request")
    public ApiResponse<List<DrawSummaryResponse>> cancel(@RequestBody @Valid CancelDrawRequest request) {
        var drawIds = requireDrawIds(request.drawIds());
        commandBus.execute(new CancelDrawCommand(drawIds, request.reasonCode(), request.reasonLabel(), request.force()));
        return ApiResponse.success(reload(drawIds));
    }

    @Operation(summary = "Lock multiple draws")
    @PostMapping("/lock")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_LOCK,
        idExpression = "#request.drawIds().toString()",
        detailsExpression = "#request")
    public ApiResponse<List<DrawSummaryResponse>> lock(@RequestBody @Valid LockDrawRequest request) {
        var drawIds = requireDrawIds(request.drawIds());
        commandBus.execute(new LockDrawCommand(drawIds, request.reason()));
        return ApiResponse.success(reload(drawIds));
    }

    @Operation(summary = "Unlock multiple draws")
    @PostMapping("/unlock")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_UNLOCK,
        idExpression = "#request.drawIds().toString()",
        detailsExpression = "#request")
    public ApiResponse<List<DrawSummaryResponse>> unlock(@RequestBody @Valid UnlockDrawRequest request) {
        var drawIds = requireDrawIds(request.drawIds());
        commandBus.execute(new UnlockDrawCommand(drawIds, request.reason()));
        return ApiResponse.success(reload(drawIds));
    }

    @Operation(summary = "Archive multiple draws")
    @PostMapping("/archive")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_ARCHIVE,
        idExpression = "#request.drawIds().toString()",
        detailsExpression = "#request")
    public ApiResponse<List<DrawSummaryResponse>> archive(@RequestBody @Valid ArchiveDrawRequest request) {
        var drawIds = requireDrawIds(request.drawIds());
        commandBus.execute(new ArchiveDrawCommand(drawIds, request.reason(), request.force()));
        return ApiResponse.success(reload(drawIds));
    }

    @Operation(summary = "Settle multiple draws")
    @PostMapping("/settle")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AuditLog(
        entity = AuditEntityType.DRAW,
        action = AuditAction.DRAW_SETTLE,
        idExpression = "#request.drawIds().toString()",
        detailsExpression = "#request")
    public ApiResponse<List<DrawSummaryResponse>> settle(@RequestBody @Valid SettleDrawRequest request) {
        var drawIds = requireDrawIds(request.drawIds());
        commandBus.execute(new SettleDrawCommand(drawIds, request.reason(), request.force()));
        return ApiResponse.success(reload(drawIds));
    }

    private List<DrawSummaryResponse> reload(List<DrawId> drawIds) {
        return drawIds.stream()
            .map(this::reload)
            .toList();
    }

    private DrawSummaryResponse reload(DrawId drawId) {
        var summary = queryBus.ask(new GetDrawByIdQuery(drawId));
        return mapper.toDrawSummaryResponse(summary);
    }

    private List<DrawId> requireDrawIds(List<DrawId> drawIds) {
        if (drawIds == null || drawIds.isEmpty()) {
            throw ProblemRest.badRequest("drawIds is required");
        }

        var normalized = drawIds.stream()
            .filter(id -> id != null)
            .distinct()
            .toList();

        if (normalized.isEmpty()) {
            throw ProblemRest.badRequest("drawIds is required");
        }
        if (normalized.size() > DrawLifecycleCommandLimits.MAX_DRAW_IDS) {
            throw ProblemRest.badRequest("drawIds cannot contain more than " + DrawLifecycleCommandLimits.MAX_DRAW_IDS + " items");
        }

        return normalized;
    }
}
