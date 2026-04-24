package com.tchalanet.server.core.drawresult.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.drawresult.application.query.model.ListDrawResultsQuery;
import com.tchalanet.server.core.drawresult.infra.web.mapper.DrawResultWebMapper;
import com.tchalanet.server.core.drawresult.infra.web.model.DrawResultResponse;
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
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Admin • Draw Results")
public class DrawResultsController {

    private final QueryBus queryBus;
    private final DrawResultWebMapper mapper;
    private final Clock clock;

    @Operation(summary = "List draw results (admin)")
    @GetMapping
    public TchPage<DrawResultResponse> list(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
        @TchPaging(
            allowedSort = {"occurredAt", "drawDate", "slotKey"},
            defaultSort = {"occurredAt,DESC"})
        TchPageRequest pageReq) {

        var page = queryBus.send(new ListDrawResultsQuery(provider, slotKey, from, to, pageReq.pageable()));
        var items = page.items().stream().map(mapper::toResponse).toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious());
    }

    @Operation(summary = "List today's draw results (admin)")
    @GetMapping("/today")
    public TchPage<DrawResultResponse> listToday(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String slotKey,
        @TchPaging(
            allowedSort = {"occurredAt", "drawDate", "slotKey"},
            defaultSort = {"occurredAt,DESC"})
        TchPageRequest pageReq) {

        var today = LocalDate.now(clock);
        var page = queryBus.send(new ListDrawResultsQuery(provider, slotKey, today, today, pageReq.pageable()));
        var items = page.items().stream().map(mapper::toResponse).toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious());
    }

    @Operation(summary = "List draw results for last N days (admin)")
    @GetMapping("/last-days")
    public TchPage<DrawResultResponse> listLastDays(
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) String slotKey,
        @RequestParam int days,
        @TchPaging(
            allowedSort = {"occurredAt", "drawDate", "slotKey"},
            defaultSort = {"occurredAt,DESC"})
        TchPageRequest pageReq) {

        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock);
        LocalDate from = to.minusDays(n - 1);

        var page = queryBus.send(new ListDrawResultsQuery(provider, slotKey, from, to, pageReq.pageable()));
        var items = page.items().stream().map(mapper::toResponse).toList();

        return TchPage.of(
            items,
            page.page(),
            page.size(),
            page.totalElements(),
            page.totalPages(),
            page.last(),
            page.hasNext(),
            page.hasPrevious());
    }

    private static int clampDays(int days) {
        // MVP guardrails
        int x = Math.max(1, days);
        return Math.min(366, x);
    }
}
