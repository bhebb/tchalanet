package com.tchalanet.server.features.publicdrawresults;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSearchCriteria;
import com.tchalanet.server.features.publicdrawresults.model.PublicDrawResultSlotsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public/draw-results")
@RequiredArgsConstructor
@Tag(name = "Public • Draw Results")
public class PublicDrawResultController {

  private final PublicDrawResultService service;

  @GetMapping("/slots")
  public ApiResponse<PublicDrawResultSlotsResponse> slots(
      @RequestParam(required = false) List<String> slotKeys,
      @RequestParam(required = false) String provider) {
    return ApiResponse.success(service.slots(slotKeys, provider));
  }

  @GetMapping("/slots/details")
  public ApiResponse<PublicDrawResultSlotsResponse> slotDetails(
      @RequestParam(required = false) List<String> slotKeys,
      @RequestParam(required = false) String provider,
      @RequestParam(required = false, defaultValue = "5") int historyLimit) {
    return ApiResponse.success(service.details(slotKeys, provider, historyLimit));
  }

  @GetMapping("/history")
  public ApiResponse<PublicDrawResultListResponse> history(
      @RequestParam(required = false) List<String> slotKeys,
      @RequestParam(required = false) String provider,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @TchPaging(
              allowedSort = {"occurredAt", "resultDate", "slotKey"},
              defaultSort = {"occurredAt,DESC"})
          TchPageRequest pageReq) {
    return ApiResponse.success(
        service.history(
            new PublicDrawResultSearchCriteria(slotKeys, provider, from, to, pageReq.pageable())));
  }
}
