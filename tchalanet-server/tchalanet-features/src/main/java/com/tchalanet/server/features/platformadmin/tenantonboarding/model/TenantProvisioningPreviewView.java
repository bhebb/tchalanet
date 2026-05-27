package com.tchalanet.server.features.platformadmin.tenantonboarding.model;

import java.util.List;

/**
 * Read-only preview of what would happen when provisioning a tenant with a
 * given profile. No data is written.
 */
public record TenantProvisioningPreviewView(
    TenantProvisioningProfile profile,
    List<String> includedDomains,
    List<String> warnings,
    List<String> notCopiedData,
    List<String> expectedReadinessSections) {}
