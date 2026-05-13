package com.tchalanet.server.core.outlet.internal.infra.web.admin;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.outlet.api.query.GetOutletByIdQuery;
import com.tchalanet.server.core.outlet.api.query.ListOutletsQuery;
import com.tchalanet.server.core.outlet.api.query.OutletSearchCriteria;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.mapper.OutletAdminWebMapper;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletResponse;
import com.tchalanet.server.core.outlet.internal.infra.web.admin.model.OutletSummaryResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/outlets")
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN','SUPER_ADMIN')")
@Tag(name = "Outlet • Query Admin")
@RequiredArgsConstructor
public class OutletAdminQueryController {

    private final QueryBus queryBus;
    private final OutletAdminWebMapper mapper;

    @GetMapping
    public ApiResponse<TchPage<OutletSummaryResponse>> list(
        @RequestParam(required = false) String q,
        @RequestParam(required = false) Boolean active,
        @RequestParam(required = false) Boolean salesBlocked,
        @RequestParam(required = false) Boolean dayClosed,
        @TchPaging(defaultSort = {"name,ASC"}, allowedSort = {"name", "slug", "createdAt"})
        TchPageRequest pageRequest) {
        OutletSearchCriteria criteria = new OutletSearchCriteria(q, active, salesBlocked, dayClosed);
        var outlets = queryBus.ask(new ListOutletsQuery(criteria, pageRequest));

        return ApiResponse.success(mapper.toSummaryResponsePage(outlets));
    }

    @GetMapping("/{id}")
    public ApiResponse<OutletResponse> get(
        @CurrentContext TchRequestContext ctx,
        @PathVariable OutletId id) {
        var outlet = queryBus.ask(new GetOutletByIdQuery(ctx.tenantIdSafe(), id));

        return ApiResponse.success(mapper.toResponse(outlet));
    }
}
