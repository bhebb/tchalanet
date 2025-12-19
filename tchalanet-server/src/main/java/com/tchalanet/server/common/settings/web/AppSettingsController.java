package com.tchalanet.server.common.settings.web;

import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.settings.dto.ResolvedSettingDto;
import com.tchalanet.server.common.settings.query.ResolveAppSettingsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
        @RequestParam List<String> namespaces
    ) {
        return ResponseEntity.ok(queryBus.send(new ResolveAppSettingsQuery(tenantId, outletId, terminalId, namespaces)));
    }
}
