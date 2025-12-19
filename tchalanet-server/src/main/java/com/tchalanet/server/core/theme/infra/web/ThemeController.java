package com.tchalanet.server.core.theme.infra.web;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.CurrentContext;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.core.theme.application.command.model.ArchiveThemeCommand;
import com.tchalanet.server.core.theme.application.command.model.PublishThemeCommand;
import com.tchalanet.server.core.theme.application.query.model.GetThemeByIdQuery;
import com.tchalanet.server.core.theme.application.query.model.ListThemesQuery;
import com.tchalanet.server.core.theme.application.query.model.ThemeView;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST API pour gérer les thèmes d'un tenant.
 */
@RestController
@RequestMapping("/api/v1/themes")
@RequiredArgsConstructor
public class ThemeController {

    private final CommandBus commandBus;
    private final QueryBus queryBus;

    @GetMapping
    @PreAuthorize("hasAuthority('TENANT_READ')")
    public ResponseEntity<List<ThemeView>> listThemes(
        @CurrentContext TchRequestContext context,
        @RequestParam(defaultValue = "false") boolean includeBase,
        @RequestParam(required = false) ThemeStatus status) {

        UUID tenantId = context.effectiveTenantUuid();
        ThemeStatus effectiveStatus = status != null ? status : ThemeStatus.PUBLISHED;

        var views =
            queryBus.send(new ListThemesQuery(tenantId, effectiveStatus, includeBase));

        return ResponseEntity.ok(views);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_READ')")
    public ResponseEntity<ThemeView> getTheme(
        @CurrentContext TchRequestContext context, @PathVariable UUID id) {

        UUID tenantId = context.effectiveTenantUuid();
        var theme = queryBus.send(new GetThemeByIdQuery(tenantId, id));

        return ResponseEntity.ok(theme);
    }


    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<Void> publishTheme(
        @CurrentContext TchRequestContext context,
        @PathVariable UUID id,
        @RequestParam Integer version) {

        UUID tenantId = context.effectiveTenantUuid();

        commandBus.send(new PublishThemeCommand(tenantId, id, version));

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public ResponseEntity<Void> archiveTheme(
        @CurrentContext TchRequestContext context, @PathVariable UUID id) {

        UUID tenantId = context.effectiveTenantUuid();
        commandBus.send(new ArchiveThemeCommand(tenantId, id));

        return ResponseEntity.noContent().build();
    }
}
