package com.tchalanet.server.features.platformadmin.overview;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Section catalogue for {@code GET /platform/overview}.
 *
 * Mirrors the spec §11 platform table (dashboard-overview-runtime-v1).
 * Routes use frontend conventions ({@code /app/platform/...}) — never backend
 * API paths.
 */
@Component
public class PlatformAdminOverviewSections {

  public List<PlatformAdminOverviewView.SectionStatusItem> items() {
    return List.of(
        // Plateforme
        new PlatformAdminOverviewView.SectionStatusItem("tenants", true, "/app/platform/tenants"),
        new PlatformAdminOverviewView.SectionStatusItem("tenant_onboarding", true, "/app/platform/tenant-onboarding"),
        new PlatformAdminOverviewView.SectionStatusItem("subscriptions", true, "/app/platform/subscriptions"),
        // Configuration globale
        new PlatformAdminOverviewView.SectionStatusItem("global_settings", true, "/app/platform/settings"),
        new PlatformAdminOverviewView.SectionStatusItem("global_i18n", true, "/app/platform/i18n"),
        new PlatformAdminOverviewView.SectionStatusItem("theme_presets", true, "/app/platform/theme-presets"),
        new PlatformAdminOverviewView.SectionStatusItem("referentials", true, "/app/platform/referentials"),
        new PlatformAdminOverviewView.SectionStatusItem("draw_channels", true, "/app/platform/draw-channels"),
        // Opérations
        new PlatformAdminOverviewView.SectionStatusItem("ops_health", true, "/app/platform/ops/health"),
        new PlatformAdminOverviewView.SectionStatusItem("ops_jobs", true, "/app/platform/ops/jobs"),
        new PlatformAdminOverviewView.SectionStatusItem("ops_cache", true, "/app/platform/ops/cache"),
        new PlatformAdminOverviewView.SectionStatusItem("audit", true, "/app/platform/audit"),
        new PlatformAdminOverviewView.SectionStatusItem("communications", true, "/app/platform/communications"),
        // Rapports
        new PlatformAdminOverviewView.SectionStatusItem("reports", true, "/app/platform/reports")
    );
  }
}
