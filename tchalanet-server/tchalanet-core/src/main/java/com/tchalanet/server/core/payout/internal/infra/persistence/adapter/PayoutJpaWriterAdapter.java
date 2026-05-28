package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.PayoutId;
import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.internal.domain.model.PayoutClaim;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutJpaEntity;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutPersistenceMapper;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PayoutJpaWriterAdapter implements PayoutWriterPort {

    private final SpringPayoutJpaRepository jpaRepo;
    private final PayoutPersistenceMapper mapper;
    private final EntityManager entityManager;

    @Override
    public PayoutClaim save(PayoutClaim claim) {
        if (claim.id() == null) {
            var entity = new PayoutJpaEntity();
            mapper.updateEntity(claim, entity);
            return mapper.toDomain(jpaRepo.save(entity));
        }

        var entity = jpaRepo.findByTenantIdAndId(claim.tenantId().value(), claim.id().value())
            .orElseThrow(() -> new IllegalStateException(
                "PayoutClaim update target not found: " + claim.id().value()));
        assertImmutableFields(entity, claim);
        mapper.updateEntity(claim, entity);
        return mapper.toDomain(entity);
    }

    @Override
    public PayoutClaim lockByIdForPayment(PayoutId payoutId) {
        var entity = entityManager.find(
            PayoutJpaEntity.class,
            payoutId.value(),
            jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (entity == null) {
            throw new TchNotFoundException(payoutId.toString(), "PayoutClaim not found for lock: ");
        }
        return mapper.toDomain(entity);
    }

    private static void assertImmutableFields(PayoutJpaEntity entity, PayoutClaim claim) {
        requireSame("payoutId", entity.getId(), claim.id().value());
        requireSame("tenantId", entity.getTenantId(), claim.tenantId().value());
        requireSame("ticketId", entity.getTicketId(), claim.ticketId().value());
        requireSame("amountCents", entity.getAmountCents(), claim.amountCents());
        requireSame("currency", entity.getCurrency(), claim.currency());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "PayoutClaim immutable field changed: "
                    + field
                    + " expected=" + actual
                    + " actual=" + expected);
        }
    }
}
