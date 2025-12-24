package com.tchalanet.server.core.limitpolicy.application.query.model;

import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;
import java.util.List;

public record GetLimitDefinitionsResult(List<LimitDefinition> definitions) {}
