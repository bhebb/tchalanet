package com.tchalanet.server.core.limitpolicy.application.port.out;

import com.tchalanet.server.common.types.enums.RuleKey;
import com.tchalanet.server.common.types.id.LimitDefinitionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.limitpolicy.domain.model.LimitDefinition;

import java.util.List;
import java.util.Optional;

public interface LimitDefinitionReaderPort {

  Optional<LimitDefinition> findById(LimitDefinitionId id);

  Optional<LimitDefinition> findByRuleKey(RuleKey ruleKey);

  List<LimitDefinition> listActive();
}
