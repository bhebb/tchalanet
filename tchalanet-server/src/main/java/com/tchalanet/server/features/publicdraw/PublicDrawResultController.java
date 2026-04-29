package com.tchalanet.server.features.publicdraw;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.publicdraw.app.PublicDrawResultDetailsService;
import com.tchalanet.server.features.publicdraw.app.PublicDrawResultSearchService;
import com.tchalanet.server.features.publicdraw.app.PublicLatestDrawResultsService;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultDetailsResponse;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdraw.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.model.PublicLatestDrawResultsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/draws/results")
@RequiredArgsConstructor
@Tag(name = "Public • Draws • Results")
public class PublicDrawResultController {

  private final PublicDrawResultSearchService searchService;
  private final PublicDrawResultDetailsService detailsService;
  private final PublicLatestDrawResultsService latestService;

  @GetMapping
  public ApiResponse<PublicDrawResultListResponse> list(
      @RequestParam(required = false) String slotKey,
      @RequestParam(required = false) String provider,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @TchPaging(
              allowedSort = {"occurredAt", "drawDate", "slotKey"},
              defaultSort = {"occurredAt,DESC"})
          TchPageRequest pageReq) {

    var criteria = new PublicDrawResultSearchCriteria(slotKey, provider, from, to, pageReq.pageable());
    return ApiResponse.success(searchService.search(criteria));
  }

  @GetMapping("/{slotKey}/{drawDate}")
  public ApiResponse<PublicDrawResultDetailsResponse> get(
      @PathVariable String slotKey, @PathVariable LocalDate drawDate) {
    return ApiResponse.success(detailsService.get(slotKey, drawDate));
  }

  @GetMapping("/latest")
  public ApiResponse<List<PublicLatestDrawResultsResponse>> latest(
      @RequestParam(defaultValue = "1") int limitPerSlot) {
    return ApiResponse.success(latestService.latest(limitPerSlot));
  }
}
