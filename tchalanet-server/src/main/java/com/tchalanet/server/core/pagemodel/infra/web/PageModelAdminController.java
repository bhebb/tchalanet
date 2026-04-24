package com.tchalanet.server.core.pagemodel.infra.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.common.bus.Command;
import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.PageModelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.core.pagemodel.application.command.model.PublishPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.command.model.UpsertPageModelCommand;
import com.tchalanet.server.core.pagemodel.application.query.model.ListPageModelsQuery;
import com.tchalanet.server.features.pagemodel.admin.dto.PageModelAdminUpsertRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/pagemodels")
@RequiredArgsConstructor
@Tag(name = "Admin • PageModel")
public class PageModelAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;
  private final ObjectMapper mapper = new ObjectMapper();

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(value = "tenantId", required = false) UUID tenantId,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "logicalId", required = false) String logicalId) {
    var res = queryBus.send(new ListPageModelsQuery(tenantId, scope, logicalId));
    return ApiResponse.success(res);
  }

  @PostMapping("/{id}/publish")
  public ApiResponse<?> publish(@PathVariable("id") String id) {
    // build typed id explicitly to avoid ambiguity in imports
    PageModelId pid = PageModelId.of(id);
    Command<Void> cmd = new PublishPageModelCommand(pid);
    commandBus.send(cmd);
    return ApiResponse.success(true);
  }

  @PostMapping
  public ApiResponse<?> create(@RequestBody PageModelAdminUpsertRequest req) {
    JsonNode modelJson = req.model() == null ? null : mapper.convertValue(req.model(), JsonNode.class);
    var cmd = new UpsertPageModelCommand(
        Optional.empty(),
        Optional.empty(), // tenantId not provided in request -> resolved from context in handler
        req.logicalId(),
        req.scope(),
        req.slug(),
        req.schemaVersion(),
        modelJson,
        Optional.empty()
    );
    Object created = commandBus.send((Command<Object>) cmd);
    return ApiResponse.success(created);
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(@PathVariable("id") String id, @RequestBody PageModelAdminUpsertRequest req) {
    PageModelId pid = PageModelId.of(id);
    JsonNode modelJson = req.model() == null ? null : mapper.convertValue(req.model(), JsonNode.class);
    var cmd = new UpsertPageModelCommand(
        Optional.of(pid),
        Optional.empty(),
        req.logicalId(),
        req.scope(),
        req.slug(),
        req.schemaVersion(),
        modelJson,
        Optional.empty()
    );
    Object updated = commandBus.send((Command<Object>) cmd);
    return ApiResponse.success(updated);
  }

  // Minimal mapper from feature PageModel to core PageModelDoc (structure compatible)
  private com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc mapToDoc(com.tchalanet.server.features.pagemodel.PageModel m) {
    if (m == null) return null;
    return new com.tchalanet.server.core.pagemodel.domain.model.PageModelDoc(null, null, null, null);
  }
}
