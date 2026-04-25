package com.tchalanet.server.core.pagemodel.infra.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.util.JsonUtils;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.core.pagemodel.application.command.model.DuplicatePageModelCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.PublishPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.ResetPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.query.model.ListPageModelsQuery;
import com.tchalanet.server.core.pagemodel.application.query.model.PreviewPageModelQuery;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminDetailDto;
import com.tchalanet.server.core.pagemodel.infra.web.dto.PageModelAdminUpsertRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

// [Phase 2A-3] extraction de tenantId/actorId depuis TchRequestContext — plus de TchContext dans les handlers
// [Phase 2C] @PreAuthorize ajouté sur tous les endpoints (analysis §BLOQUANT security)
// [Phase 3B] list() paginée via @TchPaging + Optional<TenantId>
// [Phase 3C] PageModelId.of(String) → PageModelId.parse(String)
// [Phase 5] preview/duplicate/reset — handlers retournent PageModelAdminDetailDto via mapper
@RestController
@RequestMapping("/admin/pagemodels")
@RequiredArgsConstructor
@Tag(name = "Admin • PageModel")
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
public class PageModelAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final JsonUtils jsonUtils;

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(value = "tenantId", required = false) UUID tenantId,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "logicalId", required = false) String logicalId,
      @TchPaging TchPageRequest pageReq) {
    var query = new ListPageModelsQuery(
        Optional.ofNullable(tenantId).map(TenantId::of),
        Optional.ofNullable(scope).filter(s -> !s.isBlank()),
        Optional.ofNullable(logicalId).filter(s -> !s.isBlank()),
        pageReq.pageable()
    );
    return ApiResponse.success(queryBus.send(query));
  }

  @PostMapping("/{id}/publish")
  public ApiResponse<?> publish(
      @PathVariable("id") String id,
      @CurrentContext TchRequestContext ctx) {
    PageModelId pid = PageModelId.parse(id);
    Command<Void> cmd = new PublishPageModelCommand(pid, ctx.tenantIdSafe(), ctx.userId());
    commandBus.send(cmd);
    return ApiResponse.success(true);
  }

  @PostMapping
  public ApiResponse<?> create(
      @RequestBody PageModelAdminUpsertRequest req,
      @CurrentContext TchRequestContext ctx) {
    JsonNode modelJson = req.model() == null ? null : jsonUtils.valueToTree(req.model());
    var cmd = new UpsertPageModelCommand(
        Optional.empty(),
        ctx.tenantIdSafe(),
        ctx.userId(),
        req.logicalId(),
        req.scope(),
        req.slug(),
        req.schemaVersion(),
        modelJson,
        Optional.empty()
    );
    return ApiResponse.success(commandBus.send(cmd));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(
      @PathVariable("id") String id,
      @RequestBody PageModelAdminUpsertRequest req,
      @CurrentContext TchRequestContext ctx) {
    PageModelId pid = PageModelId.parse(id);
    JsonNode modelJson = req.model() == null ? null : jsonUtils.valueToTree(req.model());
    var cmd = new UpsertPageModelCommand(
        Optional.of(pid),
        ctx.tenantIdSafe(),
        ctx.userId(),
        req.logicalId(),
        req.scope(),
        req.slug(),
        req.schemaVersion(),
        modelJson,
        Optional.empty()
    );
    return ApiResponse.success(commandBus.send(cmd));
  }

  // ------------------------------------------------------------------ preview

  @Operation(summary = "Preview a page model (admin) — retourne le PageModel tel quel sans résolution dynamique")
  @GetMapping("/{id}/preview")
  @PreAuthorize("hasPermission(null, 'pagemodel.admin.read')")
  public ApiResponse<PageModelAdminDetailDto> preview(
      @PathVariable PageModelId id
  ) {
    return ApiResponse.success(queryBus.send(new PreviewPageModelQuery(id)));
  }

  // ---------------------------------------------------------------- duplicate

  @Operation(summary = "Duplicate a page model (admin) — crée une copie DRAFT dans le même tenant")
  @PostMapping("/{id}/duplicate")
  @PreAuthorize("hasPermission(null, 'pagemodel.admin.write')")
  public ApiResponse<PageModelAdminDetailDto> duplicate(
      @PathVariable PageModelId id,
      @RequestParam(required = false) @Size(max = 128) String logicalId,
      @RequestParam(required = false) @Size(max = 128) String slug,
      @CurrentContext TchRequestContext ctx
  ) {
    var cmd = new DuplicatePageModelCommand(
        id,
        ctx.userId(),
        Optional.ofNullable(logicalId).filter(s -> !s.isBlank()),
        Optional.ofNullable(slug).filter(s -> !s.isBlank())
    );
    return ApiResponse.success(commandBus.send(cmd));
  }

  // ------------------------------------------------------------------ reset

  @Operation(summary = "Reset a page model to template defaults (admin) — repasse en DRAFT")
  @PostMapping("/{id}/reset")
  @PreAuthorize("hasPermission(null, 'pagemodel.admin.write')")
  public ApiResponse<PageModelAdminDetailDto> reset(
      @PathVariable PageModelId id,
      @CurrentContext TchRequestContext ctx
  ) {
    var cmd = new ResetPageModelCommand(id, ctx.currentUserIdRequired());
    return ApiResponse.success(commandBus.send(cmd));
  }
}
