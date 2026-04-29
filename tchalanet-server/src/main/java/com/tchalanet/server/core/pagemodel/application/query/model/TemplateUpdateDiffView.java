package com.tchalanet.server.core.pagemodel.application.query.model;

import java.util.List;

public record TemplateUpdateDiffView(
    List<String> added,
    List<String> removed,
    List<String> changed,
    List<String> conflicts) {}
