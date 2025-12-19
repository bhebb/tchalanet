package com.tchalanet.server.core.theme.application.command.handler;

import com.tchalanet.server.common.bus.VoidCommandHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.tenant.application.port.out.TenantWriterPort;
import com.tchalanet.server.core.theme.application.command.model.PublishThemeCommand;
import com.tchalanet.server.core.theme.application.port.out.ThemeReaderPort;
import com.tchalanet.server.core.theme.application.port.out.ThemeWriterPort;
import com.tchalanet.server.core.theme.domain.model.Theme;
import com.tchalanet.server.core.theme.domain.model.ThemeStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class PublishThemeCommandHandler implements VoidCommandHandler<PublishThemeCommand> {

    private final ThemeReaderPort themeReaderPort;
    private final ThemeWriterPort themeWriterPort;
    private final TenantWriterPort tenantWriterPort;
    private final Clock clock;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "publishedThemeByTenant", key = "#cmd.tenantId()")
    public void handle(PublishThemeCommand cmd) {
        UUID tenantId = require(cmd.tenantId(), "tenantId is required");
        UUID themeId = require(cmd.themeId(), "themeId is required");
        int version = cmd.themeVersion();

        if (version <= 0) {
            throw new IllegalArgumentException("themeVersion must be > 0");
        }

        Theme theme =
            themeReaderPort
                .findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("Theme not found: " + themeId));

        // Safety: theme belongs to tenant
        if (!tenantId.equals(theme.tenantId())) {
            throw new IllegalStateException(
                "Tenant mismatch for theme " + themeId + ": expected=" + tenantId + ", got=" + theme.tenantId());
        }

        // Cannot publish archived
        if (theme.status() == ThemeStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot publish archived theme: " + themeId);
        }

        // V1: allow publish only from DRAFT (recommended)
        // If you want "republish published with new version", allow it but keep a strict rule.
        if (theme.status() != ThemeStatus.DRAFT) {
            throw new IllegalStateException("Theme must be DRAFT to publish. Current=" + theme.status());
        }

        // Build new published theme:
        // - status = PUBLISHED
        // - version (domain) = themeVersion (functional)
        // - updatedAt managed by persistence, but domain can carry now if you want
        Instant now = Instant.now(clock);

        var published =
            new Theme(
                theme.id(),
                theme.tenantId(),
                theme.basePresetId(),
                theme.label(),
                theme.mode(),
                theme.density(),
                theme.palette(),
                theme.tokens(),
                theme.cssVars(),
                ThemeStatus.PUBLISHED,
                version, // theme_version (functional published revision)
                theme.createdAt(),
                now);

        // Save theme (Envers will keep audit history)
        var saved = themeWriterPort.save(published);

        // Activate it for tenant
        tenantWriterPort.setActiveThemeId(tenantId, saved.id());
    }

    private static <T> T require(T v, String msg) {
        if (v == null) throw new IllegalArgumentException(msg);
        return v;
    }
}

