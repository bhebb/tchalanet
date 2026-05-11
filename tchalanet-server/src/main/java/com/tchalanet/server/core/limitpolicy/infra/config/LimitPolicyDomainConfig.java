package com.tchalanet.server.core.limitpolicy.infra.config;

import com.tchalanet.server.core.limitpolicy.domain.engine.LimitEvaluationEngine;
import com.tchalanet.server.core.limitpolicy.domain.resolver.LimitResolver;
import com.tchalanet.server.core.limitpolicy.domain.rule.BlockBetTypeEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.BlockSelectionPerDrawEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.LimitRuleEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxLinesPerTicketEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxPotentialPayoutExposurePerSelectionPerDrawEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxPotentialPayoutPerLineEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxPotentialPayoutPerTicketEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxSalesCountPerSelectionPerDrawEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxStakeExposurePerSelectionPerDrawEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxStakePerBetTypePerTicketEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxStakePerLineEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxStakePerSelectionPerTicketEvaluator;
import com.tchalanet.server.core.limitpolicy.domain.rule.MaxStakePerTicketEvaluator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class LimitPolicyDomainConfig {

    @Bean
    public LimitResolver limitResolver() {
        return new LimitResolver();
    }

    @Bean
    public List<LimitRuleEvaluator> limitRuleEvaluators() {
        return List.of(
            new MaxStakePerLineEvaluator(),
            new MaxStakePerTicketEvaluator(),
            new MaxLinesPerTicketEvaluator(),
            new MaxStakePerBetTypePerTicketEvaluator(),
            new MaxStakePerSelectionPerTicketEvaluator(),
            new MaxPotentialPayoutPerTicketEvaluator(),
            new MaxPotentialPayoutPerLineEvaluator(),
            new BlockBetTypeEvaluator(),
            new BlockSelectionPerDrawEvaluator(),

            // exposure
            new MaxStakeExposurePerSelectionPerDrawEvaluator(),
            new MaxPotentialPayoutExposurePerSelectionPerDrawEvaluator(),
            new MaxSalesCountPerSelectionPerDrawEvaluator()
        );
    }

    @Bean
    public LimitEvaluationEngine limitEvaluationEngine(
        List<LimitRuleEvaluator> evaluators
    ) {
        return new LimitEvaluationEngine(evaluators);
    }
}
