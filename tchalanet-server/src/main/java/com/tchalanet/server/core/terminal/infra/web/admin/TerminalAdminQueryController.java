package com.tchalanet.server.core.terminal.infra.web.admin;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.terminal.application.query.model.GetTerminalByIdQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListOfflineTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListSyncPendingTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.ListTerminalsQuery;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSummaryView;
import com.tchalanet.server.core.terminal.application.query.model.TerminalView;
import com.tchalanet.server.core.terminal.domain.model.TerminalKind;
import com.tchalanet.server.core.terminal.domain.model.TerminalState;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/admin/terminals")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Terminal • Query Admin")
@RequiredArgsConstructor
public class TerminalAdminQueryController {


    private final QueryBus queryBus;


    @GetMapping
    public ApiResponse<TchPage<TerminalSummaryView>> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) OutletId outletId,
        @RequestParam(required = false) UserId assignedUserId,
        @RequestParam(required = false) TerminalKind kind,
        @RequestParam(required = false) TerminalState state,
        @RequestParam(required = false) TerminalSyncState syncState,
        @RequestParam(required = false) Boolean autoSessionEnabled,
        @TchPaging(defaultSort = {"label,ASC"}, allowedSort = {"label", "createdAt", "lastSeen"})
        TchPageRequest pageRequest) {
        var criteria =
            new TerminalSearchCriteria(q, outletId, assignedUserId, kind, state, syncState, autoSessionEnabled);
        return ApiResponse.success(queryBus.ask(new ListTerminalsQuery(criteria, pageRequest)));
    }

    @GetMapping("/{id}")
    public ApiResponse<TerminalView> get(@CurrentContext TchRequestContext context,
                                         @PathVariable TerminalId id) {
        return ApiResponse.success(queryBus.ask(new GetTerminalByIdQuery(context.tenantIdSafe(), id)));
    }

    @GetMapping("/offline")
    public ApiResponse<List<TerminalSummaryView>> listOffline() {
        return ApiResponse.success(queryBus.ask(new ListOfflineTerminalsQuery()));
    }

    @GetMapping("/sync-pending")
    public ApiResponse<List<TerminalSummaryView>> listSyncPending() {
        return ApiResponse.success(queryBus.ask(new ListSyncPendingTerminalsQuery()));
    }
}
