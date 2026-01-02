package com.tchalanet.server.core.settings.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.core.settings.query.ResolveAppSettingsQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin • App Settings")
public class AppSettingsController {

  private final QueryBus queryBus;

  @Operation(summary = "Resolve app settings for a tenant/outlet/terminal (admin)")
  @GetMapping("/resolve")
  public ResponseEntity<List<ResolvedSettingDto>> resolve(
      @RequestParam UUID tenantId,
      @RequestParam(required = false) UUID outletId,
      @RequestParam(required = false) UUID terminalId,
      @RequestParam List<String> namespaces) {
    var t = TenantId.of(tenantId);
    var o = outletId == null ? null : OutletId.of(outletId);
    var tr = terminalId == null ? null : TerminalId.of(terminalId);
    return ResponseEntity.ok(queryBus.send(new ResolveAppSettingsQuery(t, o, tr, namespaces)));
  }
}
