package com.tchalanet.server.core.payout.internal.infra.persistence.adapter;

import com.tchalanet.server.core.payout.internal.application.port.out.PayoutWriterPort;
import com.tchalanet.server.core.payout.internal.domain.model.Payout;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutJpaEntity;
import com.tchalanet.server.core.payout.internal.infra.persistence.PayoutPersistenceMapper;
import com.tchalanet.server.core.payout.internal.infra.persistence.SpringPayoutJpaRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayoutJpaWriterAdapter implements PayoutWriterPort {

    private final SpringPayoutJpaRepository jpaRepo;
    private final PayoutPersistenceMapper mapper;

    @Override
    public Payout save(Payout payout) {
        if (payout.id() == null) {
            var entity = new PayoutJpaEntity();
            mapper.updateEntity(payout, entity);
            return mapper.toDomain(jpaRepo.save(entity));
        }

        var entity = jpaRepo.findByTenantIdAndId(payout.tenantId().value(), payout.id().value())
            .orElseThrow(() -> new IllegalStateException(
                "Payout update target not found: " + payout.id().value()));
        assertImmutableFields(entity, payout);
        mapper.updateEntity(payout, entity);
        return mapper.toDomain(entity);
    }

    private static void assertImmutableFields(PayoutJpaEntity entity, Payout payout) {
        requireSame("payoutId", entity.getId(), payout.id().value());
        requireSame("tenantId", entity.getTenantId(), payout.tenantId().value());
        requireSame("ticketId", entity.getTicketId(), payout.ticketId().value());
        requireSame("amountCents", entity.getAmountCents(), payout.amountCents());
        requireSame("currency", entity.getCurrency(), payout.currency());
        requireSame(
            "sellingOutletId",
            entity.getSellingOutletId(),
            payout.sellingOutletId() == null ? null : payout.sellingOutletId().value());
        requireSame(
            "sellingSessionId",
            entity.getSellingSessionId(),
            payout.sellingSessionId() == null ? null : payout.sellingSessionId().value());
        requireSame(
            "requestedBy",
            entity.getRequestedBy(),
            payout.requestedBy() == null ? null : payout.requestedBy().value());
        requireSame("requestedAt", entity.getRequestedAt(), payout.requestedAt());
    }

    private static void requireSame(String field, Object actual, Object expected) {
        if (!Objects.equals(actual, expected)) {
            throw new IllegalStateException(
                "Payout immutable field changed: "
                    + field
                    + " expected="
                    + actual
                    + " actual="
                    + expected);
        }
    }
}
