package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.core.payout.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.domain.model.Payout;
import com.tchalanet.server.core.payout.infra.persistence.PayoutJpaEntity;
import com.tchalanet.server.core.payout.infra.persistence.PayoutPersistenceMapper;
import com.tchalanet.server.core.payout.infra.persistence.SpringPayoutJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutJpaWriterAdapter implements PayoutWriterPort {

    private final SpringPayoutJpaRepository jpaRepo;
    private final PayoutPersistenceMapper mapper;

    @Override
    public Payout save(Payout payout) {
        var entity =
            payout.getId() == null
                ? new PayoutJpaEntity()
                : jpaRepo.findById(payout.getId().value()).orElseGet(PayoutJpaEntity::new);

        mapper.updateEntity(payout, entity);

        return mapper.toDomain(jpaRepo.save(entity));
    }
}
