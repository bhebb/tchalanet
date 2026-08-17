package com.tchalanet.server.core.limitpolicy.internal.infra.config;

import com.tchalanet.server.core.limitpolicy.internal.domain.engine.LimitEvaluationEngine;
import com.tchalanet.server.core.limitpolicy.internal.domain.resolver.LimitResolver;
import com.tchalanet.server.core.limitpolicy.internal.domain.rule.BlockSelectionPerDrawEvaluator;
import com.tchalanet.server.core.limitpolicy.internal.domain.rule.LimitRuleEvaluator;
import com.tchalanet.server.core.limitpolicy.internal.domain.rule.MaxStakeExposurePerSelectionPerDrawEvaluator;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LimitPolicyDomainConfig {

  @Bean
  public LimitResolver limitResolver() {
    return new LimitResolver();
  }

  @Bean
  public List<LimitRuleEvaluator> limitRuleEvaluators() {
    return List.of(
        new BlockSelectionPerDrawEvaluator(), new MaxStakeExposurePerSelectionPerDrawEvaluator());
  }

  @Bean
  public LimitEvaluationEngine limitEvaluationEngine(List<LimitRuleEvaluator> evaluators) {
    return new LimitEvaluationEngine(evaluators);
  }
}
