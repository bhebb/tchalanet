package com.tchalanet.server.common.web.dto;

import java.util.List;
import java.util.Map;

public record ContextDto(
    List<String> features, Map<String, String> i18nOverrides, Map<String, Object> theme) {}
