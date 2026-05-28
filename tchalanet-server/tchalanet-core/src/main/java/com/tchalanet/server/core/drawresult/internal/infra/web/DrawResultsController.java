package com.tchalanet.server.core.drawresult.internal.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.drawresult.api.model.ResultQuality;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.drawresult.api.query.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.api.query.view.DrawResultView;
import com.tchalanet.server.core.drawresult.internal.domain.model.DrawResultStatus;
import com.tchalanet.server.core.drawresult.internal.infra.web.mapper.DrawResultWebMapper;
import com.tchalanet.server.core.drawresult.internal.infra.web.model.DrawResultResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;

@RestController
@RequestMapping("/admin/draw-results")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Admin • Draw Results")
public class DrawResultsController {

    private final QueryBus queryBus;
    private final DrawResultWebMapper mapper;
    private final Clock clock;

    @Operation(summary = "List draw results")
    @GetMapping
    public ApiResponse<TchPage<DrawResultResponse>> list(
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) DrawResultStatus status,
        @RequestParam(required = false) ResultQuality quality,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,
        @TchPaging(
            allowedSort = {"occurredAt", "slotKey", "status", "quality"},
            defaultSort = {"occurredAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        var page = queryBus.ask(
            new ListDrawResultsQuery(
                slotKey,
                status,
                quality,
                from,
                to,
                pageReq.pageable()
            )
        );

        return ApiResponse.success(toResponsePage(page));
    }

    @Operation(summary = "List today's draw results")
    @GetMapping("/today")
    public ApiResponse<TchPage<DrawResultResponse>> listToday(
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) DrawResultStatus status,
        @RequestParam(required = false) ResultQuality quality,
        @TchPaging(
            allowedSort = {"occurredAt", "slotKey", "status", "quality"},
            defaultSort = {"occurredAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        var today = LocalDate.now(clock);

        var page = queryBus.ask(
            new ListDrawResultsQuery(
                slotKey,
                status,
                quality,
                today,
                today,
                pageReq.pageable()
            )
        );

        return ApiResponse.success(toResponsePage(page));
    }

    @Operation(summary = "List draw results for last N days")
    @GetMapping("/last-days")
    public ApiResponse<TchPage<DrawResultResponse>> listLastDays(
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) DrawResultStatus status,
        @RequestParam(required = false) ResultQuality quality,
        @RequestParam int days,
        @TchPaging(
            allowedSort = {"occurredAt", "slotKey", "status", "quality"},
            defaultSort = {"occurredAt,DESC"}
        ) TchPageRequest pageReq
    ) {
        var n = clampDays(days);
        var to = LocalDate.now(clock);
        var from = to.minusDays(n - 1L);

        var page = queryBus.ask(
            new ListDrawResultsQuery(
                slotKey,
                status,
                quality,
                from,
                to,
                pageReq.pageable()
            )
        );

        return ApiResponse.success(toResponsePage(page));
    }

    private TchPage<DrawResultResponse> toResponsePage(
        TchPage<DrawResultView> page
    ) {
        var items = page.items().stream()
            .map(mapper::toResponse)
            .toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    private static int clampDays(int days) {
        return Math.clamp(days, 1, 366);
    }
}
