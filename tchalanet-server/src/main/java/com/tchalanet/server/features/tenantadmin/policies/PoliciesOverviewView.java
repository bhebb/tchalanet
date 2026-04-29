package com.tchalanet.server.features.tenantadmin.policies;

public record PoliciesOverviewView(int limitDefinitionsCount, int tenantAssignmentsCount, boolean autonomyConfigured, String autonomyLevel) {}
