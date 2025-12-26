package com.tchalanet.server.core.tenant.infra.web.admin;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.enums.TenantType;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.tenant.application.command.model.ActivateTenantCommand;
import com.tchalanet.server.core.tenant.application.command.model.ArchiveTenantCommand;
import com.tchalanet.server.core.tenant.application.command.model.CreateTenantCommand;
import com.tchalanet.server.core.tenant.application.command.model.DeactivateTenantCommand;
import com.tchalanet.server.core.tenant.application.query.model.GetTenantByIdQuery;
import com.tchalanet.server.core.tenant.application.query.model.ResolveTenantIdByCodeQuery;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin-api/tenants")
@RequiredArgsConstructor
public class TenantAdminController {

  private final CommandBus commandBus;
  private final QueryBus queryBus;

  public record CreateTenantRequest(
      String code, String name, TenantType type, String timezone, String currency) {}

  @PostMapping
  public ResponseEntity<Map<String, Object>> create(@RequestBody CreateTenantRequest req) {
    java.util.UUID id =
        commandBus.send(
            new CreateTenantCommand(
                req.code(), req.name(), req.type(), req.timezone(), req.currency()));
    return ResponseEntity.ok(Map.of("id", id));
  }

  @GetMapping("/{id}")
  public ResponseEntity<java.util.UUID> get(@PathVariable TenantId id) {
    return ResponseEntity.ok(queryBus.send(new GetTenantByIdQuery(id)));
  }

  @GetMapping("/resolve")
  public ResponseEntity<Map<String, Object>> resolveByCode(@RequestParam("code") String code) {
    java.util.UUID id = queryBus.send(new ResolveTenantIdByCodeQuery(code));
    return ResponseEntity.ok(Map.of("id", id));
  }

  @PostMapping("/{id}/activate")
  public ResponseEntity<Void> activate(@PathVariable TenantId id) {
    commandBus.send(new ActivateTenantCommand(id));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/deactivate")
  public ResponseEntity<Void> deactivate(
      @PathVariable TenantId id, @RequestBody(required = false) Map<String, String> body) {
    var reason =
        body == null ? "deactivated_by_admin" : body.getOrDefault("reason", "deactivated_by_admin");
    commandBus.send(new DeactivateTenantCommand(id, reason));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/{id}/archive")
  public ResponseEntity<Void> archive(
      @PathVariable TenantId id, @RequestBody(required = false) Map<String, String> body) {
    var reason =
        body == null ? "archived_by_admin" : body.getOrDefault("reason", "archived_by_admin");
    commandBus.send(new ArchiveTenantCommand(id, reason));
    return ResponseEntity.noContent().build();
  }
}
