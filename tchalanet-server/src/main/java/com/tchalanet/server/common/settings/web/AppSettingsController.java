package com.tchalanet.server.common.settings.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.common.settings.query.ResolveAppSettingsQuery;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class AppSettingsController {

  private final QueryBus queryBus;

  @GetMapping("/resolve")
  public ResponseEntity<List<ResolvedSettingDto>> resolve(
      @RequestParam UUID tenantId,
      @RequestParam(required = false) UUID outletId,
      @RequestParam(required = false) UUID terminalId,
      @RequestParam List<String> namespaces) {
    TenantId t = TenantId.of(tenantId);
    OutletId o = outletId == null ? null : OutletId.of(outletId);
    TerminalId tr = terminalId == null ? null : TerminalId.of(terminalId);
    return ResponseEntity.ok(queryBus.send(new ResolveAppSettingsQuery(t, o, tr, namespaces)));
  }
}
