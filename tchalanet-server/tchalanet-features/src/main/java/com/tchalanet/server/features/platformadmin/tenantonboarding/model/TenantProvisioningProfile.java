package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

/**
 * Controlled provisioning profiles (dashboard-overview-runtime-v1 §tenant-provisioning).
 *
 * V1 supports three profiles. {@code CUSTOM_FROM_TENANT} is explicitly out of V1.
 *
 * Note: this profile is orthogonal to {@code catalog.tenant.TenantType}
 * (BORLETTE / RESEAU / AMBULANT) — TenantType classifies the running business,
 * the profile describes how the tenant is seeded at provisioning time only.
 */
public enum TenantProvisioningProfile {
  /** Minimal seed — identity only, no demo content. */
  MINIMAL,
  /** Haiti lottery default — pre-wired games, draw channels, pricing. */
  DEFAULT_HAITI_LOTTERY,
  /** Demo seed — full sample data for demo environments. */
  DEMO
}
