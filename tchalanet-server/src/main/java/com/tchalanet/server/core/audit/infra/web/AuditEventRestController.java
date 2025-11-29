// common.audit.web.AuditLog
package com.tchalanet.server.core.audit.infra.web;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
public class AuditEventRestController {

  // Disabled dependency on AuditQueryService until the query layer is implemented.
  public AuditEventRestController() {}

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
