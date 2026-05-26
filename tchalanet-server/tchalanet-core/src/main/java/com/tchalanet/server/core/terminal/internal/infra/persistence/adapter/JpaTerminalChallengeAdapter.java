package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalActivationChallengeId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.challenge.TerminalActivationChallengeWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.challenge.TerminalActivationChallenge;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalChallengeJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalChallengeJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalChallengeAdapter
    implements TerminalActivationChallengeReaderPort, TerminalActivationChallengeWriterPort {

    private final TerminalChallengeJpaRepository repository;

    @Override
    public Optional<TerminalActivationChallenge> findById(
        TenantId tenantId,
        TerminalActivationChallengeId challengeId
    ) {
        return repository.findByTenantIdAndId(tenantId.value(), challengeId.value())
            .map(this::toDomain);
    }

    @Override
    public TerminalActivationChallenge save(TerminalActivationChallenge challenge) {
        return toDomain(repository.save(toEntity(challenge)));
    }

    private TerminalActivationChallenge toDomain(TerminalChallengeJpaEntity entity) {
        return new TerminalActivationChallenge(
            TerminalActivationChallengeId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            TerminalId.of(entity.getTerminalId()),
            UserId.of(entity.getUserId()),
            entity.getChallengeType(),
            entity.getChannel(),
            entity.getCodeHash(),
            entity.getExpiresAt(),
            entity.getAttemptCount(),
            entity.getMaxAttempts(),
            entity.getStatus(),
            entity.getCreatedAt(),
            entity.getConsumedAt()
        );
    }

    private TerminalChallengeJpaEntity toEntity(TerminalActivationChallenge challenge) {
        var entity = new TerminalChallengeJpaEntity();
        entity.setId(challenge.id().value());
        entity.setTenantId(challenge.tenantId().value());
        entity.setTerminalId(challenge.terminalId().value());
        entity.setUserId(challenge.userId().value());
        entity.setChallengeType(challenge.challengeType());
        entity.setChannel(challenge.channel());
        entity.setCodeHash(challenge.codeHash());
        entity.setExpiresAt(challenge.expiresAt());
        entity.setAttemptCount(challenge.attemptCount());
        entity.setMaxAttempts(challenge.maxAttempts());
        entity.setStatus(challenge.status());
        entity.setConsumedAt(challenge.consumedAt());
        return entity;
    }
}
