package com.tchalanet.server.core.pagemodel.api.query;

import java.util.List;

public record TemplateUpdateDiffView(
    List<String> added,
    List<String> removed,
    List<String> changed,
    List<String> conflicts) {}
