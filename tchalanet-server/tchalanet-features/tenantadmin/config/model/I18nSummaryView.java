package com.tchalanet.server.features.tenantadmin.config.model;

import java.util.List;
import java.util.Map;

public record I18nSummaryView(List<String> locales, Map<String, Long> countByLocale) {}
