package com.tchalanet.server.features.pagemodel.dynamic.providers.platformadmin;

public interface OpsResourceMetricsProvider {

  PlatformAdminOpsDashboardPayloadAssembler.OpsResourceSummaryPayload snapshot();
}
