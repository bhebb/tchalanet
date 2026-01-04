package com.tchalanet.server.common.settings.web;

import com.tchalanet.server.common.batch.BatchGate;
import com.tchalanet.server.common.types.id.TenantId;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/platform/ops/batch")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@Tag(name = "Ops • Batch Admin")
public class BatchFlagsReadController {

  private final BatchGate gate;

  @GetMapping("/effective")
  public Map<String, Boolean> effective(@RequestParam(required = false) TenantId tenantId) {
    return Map.of(
        "enabled", gate.enabled("enabled", tenantId),
        "draw.generate.enabled", gate.enabled("draw.generate.enabled", tenantId),
        "draw.open.enabled", gate.enabled("draw.open.enabled", tenantId),
        "draw.close.enabled", gate.enabled("draw.close.enabled", tenantId),
        "results.fetch.enabled", gate.enabled("results.fetch.enabled", tenantId),
        "results.apply.enabled", gate.enabled("results.apply.enabled", tenantId)
    );
  }
}

