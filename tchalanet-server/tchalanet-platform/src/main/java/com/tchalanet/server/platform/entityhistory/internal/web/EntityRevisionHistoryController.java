package com.tchalanet.server.platform.entityhistory.internal.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.entityhistory.internal.service.EntityRevisionHistoryService;
import com.tchalanet.server.platform.entityhistory.internal.service.EntityRevisionItem;
import com.tchalanet.server.platform.entityhistory.internal.service.TechnicalRevisionEntityType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequestMapping("/platform/entity-history")
@Tag(name = "Platform • Entity history")
public class EntityRevisionHistoryController {

  private final EntityRevisionHistoryService service;

  @Operation(summary = "Fetch technical entity revisions")
  @GetMapping("/revisions")
  public ApiResponse<TchPage<EntityRevisionItem>> listRevisions(
      @RequestParam TechnicalRevisionEntityType entityType,
      @RequestParam String entityId,
      @TchPaging(
          allowedSort = {"changedAt"},
          defaultSort = {"changedAt,DESC"})
          TchPageRequest pageReq) {
    return ApiResponse.success(service.listRevisions(entityType, entityId, pageReq.pageable()));
  }
}
