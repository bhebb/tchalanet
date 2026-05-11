package com.tchalanet.server.core.terminal.infra.persistence.adapter;

import com.tchalanet.server.common.error.NotFoundException;
import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSummaryView;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalMapper;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JpaTerminalReaderAdapter implements TerminalReaderPort {

    private final TerminalJpaRepository repo;
    private final TerminalMapper mapper;

    @Override
    public Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId) {
        return repo.findByTenantIdAndId(tenantId.value(), terminalId.value())
            .map(mapper::toDomain);
    }

    @Override
    public Terminal getById(TenantId tenantId, TerminalId terminalId) {
        return repo.findByTenantIdAndId(tenantId.value(), terminalId.value())
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Terminal not found: " + terminalId));
    }

    @Override
    public List<Terminal> listByOutlet(
        TenantId tenantId,
        OutletId outletId,
        PageRequest pageRequest) {
        return repo.findAll(TerminalSpecifications.byOutlet(outletId), pageRequest).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public List<Terminal> listByTenant(TenantId tenantId, PageRequest pageRequest) {
        return repo.findAll(TerminalSpecifications.all(), pageRequest).stream()
            .map(mapper::toDomain)
            .toList();
    }

    @Override
    public TchPage<TerminalSummaryView> search(
        TerminalSearchCriteria criteria,
        TchPageRequest pageRequest) {
        return TchPageMapper.map(
            repo.findAll(TerminalSpecifications.matching(criteria), pageRequest.pageable()),
            mapper::toSummaryView);
    }

    @Override
    public List<TerminalSummaryView> listOffline() {
        return repo.findBySyncState(TerminalSyncState.OFFLINE.name()).stream()
            .map(mapper::toSummaryView)
            .toList();
    }

    @Override
    public List<TerminalSummaryView> listSyncPending() {
        return repo.findBySyncState(TerminalSyncState.SYNC_PENDING.name()).stream()
            .map(mapper::toSummaryView)
            .toList();
    }

    @Override
    public Optional<Terminal> findCurrentForUser(UserId userId) {
        return repo.findFirstByAssignedUserIdAndAutoSessionEnabledIsTrue(userId.value())
            .map(mapper::toDomain);
    }
}
