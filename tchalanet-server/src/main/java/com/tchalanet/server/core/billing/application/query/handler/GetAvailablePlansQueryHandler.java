package com.tchalanet.server.core.billing.application.query.handler;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.billing.domain.model.Plan;
import com.tchalanet.server.core.billing.infra.persistence.PlanRestRepository;
import com.tchalanet.server.core.billing.infra.persistence.mapper.PlanPersistenceMapper;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class GetAvailablePlansQueryHandler implements QueryHandler<UUID, List<Plan>> {

    private final PlanRestRepository planRestRepository;
    private final PlanPersistenceMapper planPersistenceMapper;

    @Override
    public List<Plan> handle(UUID planId) {
        if (planId != null) {
            return planPersistenceMapper.toDomains(List.of(planRestRepository.findById(planId).get()));
        }

        return planPersistenceMapper.toDomains(planRestRepository.findAll());
    }
}
