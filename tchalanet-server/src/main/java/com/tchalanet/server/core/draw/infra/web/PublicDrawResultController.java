package com.tchalanet.server.core.draw.infra.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.core.draw.application.query.model.GetLatestPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.application.query.model.GetPublicDrawResultQuery;
import com.tchalanet.server.core.draw.application.query.model.ListPublicDrawResultsQuery;
import com.tchalanet.server.core.draw.infra.web.model.GetLatestPublicDrawResultsRequest;
import com.tchalanet.server.core.draw.infra.web.model.GetPublicDrawResultRequest;
import com.tchalanet.server.core.draw.infra.web.model.ListPublicDrawResultsRequest;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultItemResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicDrawResultPageResponse;
import com.tchalanet.server.core.draw.infra.web.model.PublicLatestDrawResultsResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
  public PublicDrawResultPageResponse list(
      @RequestParam(required = false) String channelCode,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    size = Math.min(size, 100);
    var request = new ListPublicDrawResultsRequest(channelCode, from, to, page, size);
    var query =
        new ListPublicDrawResultsQuery(
            request.channelCode(),
            request.from(),
            request.to(),
            PageRequest.of(request.page(), request.size(), Sort.by("drawDate").descending()));
    return queryBus.send(query);
  }

  @GetMapping("/{channelCode}/{drawDate}")
  public PublicDrawResultItemResponse get(
      @PathVariable String channelCode, @PathVariable LocalDate drawDate) {
    var request = new GetPublicDrawResultRequest(channelCode, drawDate);
    var query = new GetPublicDrawResultQuery(request.channelCode(), request.drawDate());
    return queryBus.send(query);
  }

  @GetMapping("/latest")
  public List<PublicLatestDrawResultsResponse> latest(
      @RequestParam(defaultValue = "1") int limitPerChannel) {
    var request = new GetLatestPublicDrawResultsRequest(limitPerChannel);
    var query = new GetLatestPublicDrawResultsQuery(request.limitPerChannel());
    return queryBus.send(query);
  }
}
