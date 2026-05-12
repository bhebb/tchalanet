package com.tchalanet.server.platform.audit.internal.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.AuditAction;
import com.tchalanet.server.common.types.enums.AuditEntityType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.audit.api.AuditLog;
import com.tchalanet.server.platform.audit.api.model.ListAuditEventsQuery;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsCommand;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import com.tchalanet.server.platform.audit.internal.service.AuditEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequestMapping("/platform/audit")
@Tag(name = "Platform • Audit")
public class AuditEventRestController {

  private final QueryBus queryBus;
  private final CommandBus commandBus;

  @Operation(summary = "Fetch audit logs")
  @GetMapping({"", "/logs"})
  public ApiResponse<TchPage<AuditEventResponse>> getAuditLogs(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) AuditEntityType entityType,
      @RequestParam(required = false) String entityId,
      @RequestParam(required = false) AuditAction action,
      @RequestParam(required = false) String actorId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
      @TchPaging(
          allowedSort = {"occurredAt", "action", "entityType", "entityId", "actorId"},
          defaultSort = {"occurredAt,DESC"})
          TchPageRequest pageReq) {

    var page = queryBus.ask(new ListAuditEventsQuery(
        TenantId.nullableOf(tenantId),
        entityType,
        entityId,
        action,
        actorId,
        from,
        to,
        pageReq.pageable()));

    return ApiResponse.success(TchPageMapper.map(page, this::toResponse));
  }

  @Operation(summary = "Purge expired audit logs")
  @PostMapping("/purge")
  @AuditLog(
      entity = AuditEntityType.SYSTEM,
      action = AuditAction.AUDIT_PURGE,
      idExpression = "'audit_event'",
      detailsExpression = "#result")
  public ApiResponse<PurgeOldAuditEventsResult> purgeExpiredAuditLogs() {
    return ApiResponse.success(commandBus.execute(new PurgeOldAuditEventsCommand()));
  }

  private AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(
        event.id(),
        event.tenantId() == null ? null : event.tenantId().value(),
        event.occurredAt(),
        event.actorType(),
        event.actorId(),
        event.entityType(),
        event.entityId(),
        event.action(),
        event.detailsJson(),
        event.ip(),
        event.userAgent());
  }
}
