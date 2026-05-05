package com.tchalanet.server.features.publicdraw;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultDetailsResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsPageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/public/draws/results")
@RequiredArgsConstructor
@Tag(name = "Public • Draws • Results")
public class PublicDrawResultController {

    private final PublicDrawResultService service;

    @GetMapping
    public ApiResponse<PublicDrawResultListResponse> list(
        @RequestParam(required = false) String slotKey,
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @TchPaging(
            allowedSort = {"resultedAt", "drawDate", "resultSlotKey"},
            defaultSort = {"resultedAt,DESC"})
        TchPageRequest pageReq) {

        var criteria =
            new PublicDrawResultSearchCriteria(
                slotKey,
                provider,
                from,
                to,
                pageReq.pageable());

        return ApiResponse.success(service.search(criteria));
    }

    @GetMapping("/{drawId}")
    public ApiResponse<PublicDrawResultDetailsResponse> getByDrawId(
        @PathVariable String drawId) {

        return ApiResponse.success(service.getByDrawId(drawId));
    }

    @GetMapping("/latest")
    public ApiResponse<PublicLatestDrawResultsPageResponse> latest(
        @RequestParam(required = false) List<String> slotKeys,
        @RequestParam(defaultValue = "1") int limitPerSlot,
        @TchPaging(
            allowedSort = {"resultedAt", "drawDate", "resultSlotKey"},
            defaultSort = {"resultedAt,DESC"})
        TchPageRequest pageReq) {

        return ApiResponse.success(
            service.latest(limitPerSlot, slotKeys, pageReq.pageable()));
    }
}
