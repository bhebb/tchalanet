package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.common.types.id.DrawResultId;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultDetailResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultHistoryResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultLatestResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSearchCriteria;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/public/draw-results")
@RequiredArgsConstructor
@Tag(name = "Public • Draw Results")
public class PublicDrawResultController {

    private final PublicDrawResultService service;

    /**
     * Home/public widget: latest result per public slot + next expected result time.
     * Sans limit, retourne tous les slots publics actifs.
     * Avec limit, coupe volontairement le résultat (ex: limit=6 pour afficher seulement 6 cards).
     */
    @GetMapping("/latest")
    public ApiResponse<PublicDrawResultLatestResponse> latest(
        @RequestParam(required = false) List<String> slotKeys,
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) @Min(1) @Max(100) Integer limit) {
        return ApiResponse.success(service.latest(slotKeys, provider, limit));
    }

    /**
     * Public paginated history for /public/results.
     */
    @GetMapping("/history")
    public ApiResponse<PublicDrawResultHistoryResponse> history(
        @RequestParam(required = false) List<String> slotKeys,
        @RequestParam(required = false) String provider,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to,
        @TchPaging(
            allowedSort = {"occurredAt", "resultDate", "slotKey"},
            defaultSort = {"occurredAt,desc"})
        TchPageRequest pageReq) {
        return ApiResponse.success(
            service.history(
                new PublicDrawResultSearchCriteria(slotKeys, provider, from, to, pageReq.pageable())));
    }

    /**
     * Public detail for one draw result.
     */
    @GetMapping("/{drawResultId}")
    public ApiResponse<PublicDrawResultDetailResponse> detail(
        @PathVariable DrawResultId drawResultId) {
        return ApiResponse.success(service.detail(drawResultId));
    }
}
