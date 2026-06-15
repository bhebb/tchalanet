package com.tchalanet.server.core.payout.internal.infra.web;

import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.web.CurrentContext;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.payout.api.query.GetPayoutDetailsQuery;
import com.tchalanet.server.core.payout.api.query.ListPayoutsQuery;
import com.tchalanet.server.core.payout.internal.infra.web.mapper.PayoutWebMapper;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutDetailsResponse;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutRowResponse;
import com.tchalanet.server.core.payout.internal.infra.web.model.PayoutSearchRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/payouts")
@RequiredArgsConstructor
@Tags({@Tag(name = "Payouts • Query Admin")})
@PreAuthorize("hasAnyRole('TENANT_OWNER', 'TENANT_ADMIN', 'SUPER_ADMIN')")
public class PayoutAdminQueryController {

    private final QueryBus queryBus;
    private final PayoutWebMapper mapper;

    @GetMapping
    public ApiResponse<TchPage<PayoutRowResponse>> list(
        @CurrentContext TchRequestContext ctx,
        @TchPaging(
            allowedSort = {"createdAt", "updatedAt", "status", "amount"},
            defaultSort = {"createdAt,desc"})
        TchPageRequest page,
        @ModelAttribute PayoutSearchRequest filters) {

        var result =
            queryBus.ask(
                new ListPayoutsQuery(
                    filters.status(),
                    filters.ticketId(),
                    filters.outletId(),
                    filters.sessionId(),
                    filters.from(),
                    filters.to(),
                    page.pageable()));

        return ApiResponse.success(TchPageMapper.map(result, mapper::toResponse));
    }

    @GetMapping("/{payoutId}")
    public ApiResponse<PayoutDetailsResponse> get(
        @CurrentContext TchRequestContext ctx,
        @PathVariable PayoutId payoutId) {

        var result =
            queryBus.ask(
                new GetPayoutDetailsQuery(payoutId));

        return ApiResponse.success(mapper.toResponse(result));
    }
}
