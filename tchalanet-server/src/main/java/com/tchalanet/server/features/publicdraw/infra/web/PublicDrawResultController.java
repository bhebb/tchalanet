package com.tchalanet.server.features.publicdraw.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.features.publicdraw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.application.query.model.GetPublicDrawResultQuery;
import com.tchalanet.server.features.publicdraw.application.query.model.ListPublicDrawResultsQuery;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicDrawResultListResponse;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicLatestDrawResultsResponse;
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

  private final QueryBus queryBus;

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

    var query = new ListPublicDrawResultsQuery(slotKey, provider, from, to, pageReq.pageable());
    return ApiResponse.success(queryBus.send(query));
  }

  @GetMapping("/{slotKey}/{drawDate}")
  public ApiResponse<PublicDrawResultItemResponse> get(
      @PathVariable String slotKey, @PathVariable LocalDate drawDate) {
    return ApiResponse.success(queryBus.send(new GetPublicDrawResultQuery(slotKey, drawDate)));
  }

  @GetMapping("/latest")
  public ApiResponse<List<PublicLatestDrawResultsResponse>> latest(
      @RequestParam(defaultValue = "1") int limitPerSlot) {
    return ApiResponse.success(queryBus.send(new GetLatestPublicDrawResultsQuery(limitPerSlot)));
  }
}
