// common.audit.web.AuditLog
package com.tchalanet.server.core.audit.infra.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequestMapping("/admin/audit")
@Tag(name = "Admin • Audit")
public class AuditEventRestController {

  // Disabled dependency on AuditQueryService until the query layer is implemented.
  public AuditEventRestController() {}

  @Operation(summary = "Fetch audit logs (admin) - placeholder")
  @GetMapping("/logs")
  public List<Map<String, Object>> getAuditLogs(
      @RequestParam(value = "entity", required = false) String entity,
      @RequestParam(value = "action", required = false) String action,
      @RequestParam(value = "id", required = false) String id,
      @RequestParam(value = "details", required = false) String details) {

    // Temporary placeholder: return empty list until query service is implemented
    return List.of();
  }
}
