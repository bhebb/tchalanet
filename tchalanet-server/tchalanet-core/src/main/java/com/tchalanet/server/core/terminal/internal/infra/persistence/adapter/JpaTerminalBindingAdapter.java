package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalBindingId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.binding.TerminalDeviceBindingWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalBindingStatus;
import com.tchalanet.server.core.terminal.internal.domain.model.binding.TerminalDeviceBinding;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalBindingJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalBindingJpaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalBindingAdapter implements TerminalDeviceBindingReaderPort, TerminalDeviceBindingWriterPort {

    private final TerminalBindingJpaRepository repository;

    @Override
    public List<TerminalDeviceBinding> findActiveByTerminal(TenantId tenantId, TerminalId terminalId) {
        return repository.findByTenantIdAndTerminalIdAndStatus(
            tenantId.value(),
            terminalId.value(),
            TerminalBindingStatus.ACTIVE
        ).stream().map(this::toDomain).toList();
    }

    @Override
    public TerminalDeviceBinding save(TerminalDeviceBinding binding) {
        return toDomain(repository.save(toEntity(binding)));
    }

    private TerminalDeviceBinding toDomain(TerminalBindingJpaEntity entity) {
        return new TerminalDeviceBinding(
            TerminalBindingId.of(entity.getId()),
            TenantId.of(entity.getTenantId()),
            TerminalId.of(entity.getTerminalId()),
            entity.getBindingType(),
            entity.getStatus(),
            entity.getBindingPublicKey(),
            entity.getBindingSecretHash(),
            entity.getDeviceFingerprintHash(),
            entity.getBoundAt(),
            entity.getExpiresAt(),
            entity.getRevokedAt(),
            entity.getLastSeenAt()
        );
    }

    private TerminalBindingJpaEntity toEntity(TerminalDeviceBinding binding) {
        var entity = new TerminalBindingJpaEntity();
        entity.setId(binding.id().value());
        entity.setTenantId(binding.tenantId().value());
        entity.setTerminalId(binding.terminalId().value());
        entity.setBindingType(binding.bindingType());
        entity.setStatus(binding.status());
        entity.setBindingPublicKey(binding.bindingPublicKey());
        entity.setBindingSecretHash(binding.bindingSecretHash());
        entity.setDeviceFingerprintHash(binding.deviceFingerprintHash());
        entity.setBoundAt(binding.boundAt());
        entity.setExpiresAt(binding.expiresAt());
        entity.setRevokedAt(binding.revokedAt());
        entity.setLastSeenAt(binding.lastSeenAt());
        return entity;
    }
}
