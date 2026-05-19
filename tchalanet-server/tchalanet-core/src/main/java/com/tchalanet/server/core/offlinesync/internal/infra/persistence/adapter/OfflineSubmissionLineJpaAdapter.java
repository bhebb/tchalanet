package com.tchalanet.server.core.offlinesync.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.IdGenerator;
import com.tchalanet.server.common.types.id.OfflineSubmissionId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.money.CurrencyCode;
import com.tchalanet.server.common.types.money.Money;
import com.tchalanet.server.core.offlinesync.api.event.OfflineSubmissionLineSnapshot;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionLineReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSubmissionLineWriterPort;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionLineJpaEntity;
import com.tchalanet.server.core.offlinesync.internal.infra.persistence.OfflineSubmissionLineJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OfflineSubmissionLineJpaAdapter
    implements OfflineSubmissionLineReaderPort, OfflineSubmissionLineWriterPort {

    private static final String STATUS_PENDING = "PENDING";

    private final OfflineSubmissionLineJpaRepository repo;
    private final IdGenerator idGenerator;

    @Override
    public void saveAll(TenantId tenantId, OfflineSubmissionId submissionId,
                        List<OfflineSubmissionLineSnapshot> lines) {
        var entities = lines.stream().map(l -> {
            var e = new OfflineSubmissionLineJpaEntity();
            e.setId(idGenerator.newUuid());
            e.setTenantId(tenantId.value());
            e.setSubmissionId(submissionId.value());
            e.setLineNo(l.lineNo());
            e.setGameCode(l.gameCode());
            e.setBetType(l.betType());
            e.setBetOption(l.betOption() == null ? "" : l.betOption());
            e.setSelectionKey(l.selectionKey());
            e.setStakeAmount(l.stakeAmount().amount());
            e.setPotentialPayout(l.potentialPayout() != null ? l.potentialPayout().amount() : null);
            e.setStatus(STATUS_PENDING);
            return e;
        }).toList();
        repo.saveAll(entities);
    }

    @Override
    public List<OfflineSubmissionLineSnapshot> findBySubmissionId(
        OfflineSubmissionId submissionId, CurrencyCode currency
    ) {
        return repo.findAllBySubmissionIdOrderByLineNoAsc(submissionId.value()).stream()
            .map(e -> new OfflineSubmissionLineSnapshot(
                e.getLineNo(),
                e.getGameCode(),
                e.getBetType(),
                e.getBetOption(),
                e.getSelectionKey(),
                new Money(e.getStakeAmount(), currency),
                e.getPotentialPayout() != null ? new Money(e.getPotentialPayout(), currency) : null
            ))
            .toList();
    }
}
