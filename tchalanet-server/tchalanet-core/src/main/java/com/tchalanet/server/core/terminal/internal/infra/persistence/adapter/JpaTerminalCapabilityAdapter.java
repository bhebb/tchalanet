package com.tchalanet.server.core.terminal.internal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.core.terminal.internal.application.port.out.capability.TerminalCapabilityReaderPort;
import com.tchalanet.server.core.terminal.internal.application.port.out.capability.TerminalCapabilityWriterPort;
import com.tchalanet.server.core.terminal.internal.domain.model.TerminalCapability;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalCapabilityJpaEntity;
import com.tchalanet.server.core.terminal.internal.infra.persistence.TerminalCapabilityJpaRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalCapabilityAdapter implements TerminalCapabilityReaderPort, TerminalCapabilityWriterPort {

    private final TerminalCapabilityJpaRepository repository;

    @Override
    public Set<TerminalCapability> findByTerminal(TenantId tenantId, TerminalId terminalId) {
        return repository.findByTenantIdAndTerminalId(tenantId.value(), terminalId.value()).stream()
            .map(TerminalCapabilityJpaEntity::getCapability)
            .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void replace(TenantId tenantId, TerminalId terminalId, Set<TerminalCapability> capabilities) {
        var existing = repository.findByTenantIdAndTerminalId(tenantId.value(), terminalId.value());
        var wanted = capabilities == null ? Set.<TerminalCapability>of() : Set.copyOf(capabilities);
        var existingCapabilities = existing.stream()
            .map(TerminalCapabilityJpaEntity::getCapability)
            .collect(Collectors.toSet());

        existing.stream()
            .filter(entity -> !wanted.contains(entity.getCapability()))
            .forEach(entity -> entity.softDelete(null, java.time.Instant.now()));

        wanted.stream()
            .filter(capability -> !existingCapabilities.contains(capability))
            .map(capability -> entity(tenantId, terminalId, capability))
            .forEach(repository::save);
    }

    private static TerminalCapabilityJpaEntity entity(
        TenantId tenantId,
        TerminalId terminalId,
        TerminalCapability capability
    ) {
        var entity = new TerminalCapabilityJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId.value());
        entity.setTerminalId(terminalId.value());
        entity.setCapability(capability);
        return entity;
    }
}
