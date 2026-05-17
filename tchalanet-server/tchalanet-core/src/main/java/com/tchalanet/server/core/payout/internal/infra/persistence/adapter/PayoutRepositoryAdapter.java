package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.common.types.id.TicketId;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutReaderPort;
import com.tchalanet.server.core.payout.internal.domain.model.Payout;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutPersistenceMapper;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PayoutRepositoryAdapter implements PayoutReaderPort {

    private final SpringPayoutJpaRepository jpaRepo;
    private final PayoutPersistenceMapper mapper;

    @Override
    public Optional<Payout> findByTicketId(TicketId ticketId) {
        return jpaRepo.findByTicketId(ticketId.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Payout> findById(PayoutId payoutId) {
        return jpaRepo.findById(payoutId.value()).map(mapper::toDomain);
    }

    @Override
    public Payout getById(PayoutId payoutId) {
        return findById(payoutId)
            .orElseThrow(() -> new TchNotFoundException(payoutId.toString(), "Payout not found: "));
    }
}
