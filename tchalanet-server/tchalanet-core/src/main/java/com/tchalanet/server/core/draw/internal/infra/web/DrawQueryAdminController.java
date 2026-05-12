package com.tchalanet.server.core.draw.internal.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.common.types.id.ResultSlotId;
import com.tchalanet.server.common.apiresponse.ApiResponse;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.common.paging.TchPageMapper;
import com.tchalanet.server.common.paging.TchPageRequest;
import com.tchalanet.server.common.paging.TchPaging;
import com.tchalanet.server.core.draw.application.query.model.*;
import com.tchalanet.server.core.draw.application.query.projection.DrawSummary;
import com.tchalanet.server.core.draw.infra.web.mapper.DrawAdminWebMapper;
import com.tchalanet.server.core.draw.infra.web.model.DrawSearchRequest;
import com.tchalanet.server.core.draw.infra.web.model.DrawSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/draws")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('TENANT_ADMIN', 'SUPER_ADMIN')")
@Tag(name = "Draws • Admin")
public class DrawQueryAdminController {

    private final QueryBus queryBus;
    private final DrawAdminWebMapper mapper;
    private final Clock clock;

    @Operation(summary = "List draws")
    @GetMapping
    public ApiResponse<TchPage<DrawSummaryResponse>> listDraws(
        @Valid DrawSearchRequest request,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status", "createdAt"},
            defaultSort = {"scheduledAt,desc"})
        TchPageRequest pageReq) {

        var criteria = DrawSearchCriteria.of(
            request.resultSlotId(),
            request.status(),
            request.from(),
            request.to());

        TchPage<DrawSummary> page = queryBus.ask(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List today's draws")
    @GetMapping("/today")
    public ApiResponse<TchPage<DrawSummaryResponse>> todayDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        var criteria = DrawSearchCriteria.today(resultSlotId, LocalDate.now(clock));
        TchPage<DrawSummary> page = queryBus.ask(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List upcoming draws")
    @GetMapping("/upcoming")
    public ApiResponse<TchPage<DrawSummaryResponse>> upcomingDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @RequestParam(defaultValue = "7") int days,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "cutoffAt", "status"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        if (days < 1 || days > 30) {
            throw ProblemRest.badRequest("draw.lookahead_days_invalid");
        }

        var criteria = DrawSearchCriteria.upcoming(resultSlotId, LocalDate.now(clock), days);
        TchPage<DrawSummary> page = queryBus.ask(new ListDrawsQuery(criteria, pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List next draws")
    @GetMapping("/next")
    public ApiResponse<TchPage<DrawSummaryResponse>> nextDraws(
        @RequestParam(required = false) ResultSlotId resultSlotId,
        @RequestParam(defaultValue = "24") int lookaheadHours,
        @RequestParam(defaultValue = "1") int limitPerChannel,
        @TchPaging(
            allowedSort = {"scheduledAt", "cutoffAt", "drawDate"},
            defaultSort = {"scheduledAt,asc"})
        TchPageRequest pageReq) {

        TchPage<DrawSummary> page = queryBus.ask(new ListNextDrawsQuery(
            resultSlotId,
            lookaheadHours,
            limitPerChannel,
            pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "List latest draws with applied results")
    @GetMapping("/latest-with-results")
    public ApiResponse<TchPage<DrawSummaryResponse>> latestWithResults(
        @RequestParam(required = false) List<String> resultSlotKeys,
        @TchPaging(
            allowedSort = {"drawDate", "scheduledAt", "resultOccurredAt"},
            defaultSort = {"drawDate,desc", "scheduledAt,desc"})
        TchPageRequest pageReq) {

        TchPage<DrawSummary> page = queryBus.ask(new ListLatestDrawsWithResultsQuery(
            resultSlotKeys,
            pageReq.pageable()));

        return ApiResponse.success(TchPageMapper.map(page, mapper::toDrawSummaryResponse));
    }

    @Operation(summary = "Get draw by id")
    @GetMapping("/{drawId}")
    public ApiResponse<DrawSummaryResponse> getDraw(
        @PathVariable DrawId drawId) {

        return ApiResponse.success(reload(drawId));
    }


    private DrawSummaryResponse reload(DrawId drawId) {
        var summary = queryBus.ask(new GetDrawByIdQuery(drawId));
        return mapper.toDrawSummaryResponse(summary);
    }
}
