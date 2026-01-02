package com.tchalanet.server.core.limitpolicy.infra.web.policy;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.application.command.model.CreateLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.DeleteLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.command.model.UpdateLimitDefinitionCommand;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitDefinitionsQuery;
import com.tchalanet.server.core.limitpolicy.application.query.model.GetLimitDefinitionsResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenant/limits")
@RequiredArgsConstructor
@Tag(name = "Tenant • Limits")
public class LimitPolicyController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  @Operation(summary = "Get limit definitions (tenant)")
  @GetMapping
  public GetLimitDefinitionsResult getDefinitions(@RequestParam TenantId tenantId) {
    return queryBus.send(new GetLimitDefinitionsQuery(tenantId));
  }

  @Operation(summary = "Create limit definition (tenant)")
  @PostMapping
  public Object createDefinition(@RequestBody CreateLimitDefinitionCommand cmd) {
    return commandBus.send(cmd);
  }

  @Operation(summary = "Update limit definition (tenant)")
  @PutMapping("/{id}")
  public Object updateDefinition(
      @PathVariable UUID id, @RequestBody UpdateLimitDefinitionCommand cmd) {
    return commandBus.send(
        new UpdateLimitDefinitionCommand(
            id,
            cmd.enabled(),
            cmd.onBreach(),
            cmd.params(),
            cmd.betTypes(),
            cmd.selectionPattern()));
  }

  @Operation(summary = "Delete limit definition (tenant)")
  @DeleteMapping("/{id}")
  public void deleteDefinition(@PathVariable UUID id, @RequestParam TenantId tenantId) {
    commandBus.send(new DeleteLimitDefinitionCommand(id));
  }
}
