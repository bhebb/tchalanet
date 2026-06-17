package com.tchalanet.server.core.offlinesync.internal.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.offlinesync.api.command.submission.ApproveOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.submission.RejectOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReplayOfflineSubmissionCommand;
import com.tchalanet.server.core.offlinesync.api.command.submission.ReplayOfflineSubmissionResult;
import com.tchalanet.server.core.offlinesync.api.model.submission.OfflineSubmissionStatus;
import com.tchalanet.server.core.offlinesync.api.query.dashboard.GetOfflineDashboardQuery;
import com.tchalanet.server.core.offlinesync.api.query.dashboard.OfflineDashboardView;
import com.tchalanet.server.core.offlinesync.api.query.submission.ListOfflineSubmissionsQuery;
import com.tchalanet.server.core.offlinesync.api.query.submission.OfflineSubmissionView;
import com.tchalanet.server.core.offlinesync.internal.infra.web.model.AdminReasonRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Admin / ops endpoints for offline submissions.
 */
@RestController
@RequestMapping("/admin/offline")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
@Tags({@Tag(name = "Offline sync • Admin")})
@Validated
public class OfflineAdminController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping("/submissions")
    public ApiResponse<List<OfflineSubmissionView>> listSubmissions(
        @CurrentContext TchRequestContext ctx,
        @RequestParam(value = "sellerUserId", required = false) UUID sellerUserId,
        @RequestParam(value = "status", required = false) Set<OfflineSubmissionStatus> statuses,
        @RequestParam(value = "limit", defaultValue = "100") int limit
    ) {
        UserId seller = sellerUserId != null ? UserId.of(sellerUserId) : ctx.currentUserIdRequired();
        var views = queryBus.ask(new ListOfflineSubmissionsQuery(
            ctx.effectiveTenantIdRequired(),
            seller,
            statuses == null ? Set.of() : statuses,
            limit
        ));
        return ApiResponse.success(views);
    }

    @PostMapping("/submissions/{submissionId}/approve")
    public ApiResponse<Void> approve(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OfflineSubmissionId submissionId,
        @Valid @RequestBody AdminReasonRequest body
    ) {
        commandBus.execute(new ApproveOfflineSubmissionCommand(
            ctx.effectiveTenantIdRequired(),
            submissionId,
            ctx.currentUserIdRequired(),
            body.reason()
        ));
        return ApiResponse.success(null);
    }

    @PostMapping("/submissions/{submissionId}/reject")
    public ApiResponse<Void> reject(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OfflineSubmissionId submissionId,
        @Valid @RequestBody AdminReasonRequest body
    ) {
        commandBus.execute(new RejectOfflineSubmissionCommand(
            ctx.effectiveTenantIdRequired(),
            submissionId,
            ctx.currentUserIdRequired(),
            body.reason()
        ));
        return ApiResponse.success(null);
    }

    @PostMapping("/submissions/{submissionId}/replay-dry-run")
    public ApiResponse<ReplayOfflineSubmissionResult> replayDryRun(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OfflineSubmissionId submissionId
    ) {
        var result = commandBus.execute(new ReplayOfflineSubmissionCommand(
            ctx.effectiveTenantIdRequired(),
            submissionId
        ));
        return ApiResponse.success(result);
    }

    @GetMapping("/dashboard")
    public ApiResponse<OfflineDashboardView> dashboard(
        @CurrentContext TchRequestContext ctx
    ) {
        var view = queryBus.ask(new GetOfflineDashboardQuery(ctx.effectiveTenantIdRequired()));
        return ApiResponse.success(view);
    }
}
