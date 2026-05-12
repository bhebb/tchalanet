package com.tchalanet.server.features.tenantadmin.policies;

public record PoliciesOverviewView(int tenantAssignmentsCount, boolean autonomyConfigured, String autonomyLevel) {
}
