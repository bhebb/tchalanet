package com.tchalanet.server.core.terminal.infra.persistence.adapter;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.application.port.out.TerminalReaderPort;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.application.query.model.TerminalSummaryView;
import com.tchalanet.server.core.terminal.domain.model.Terminal;
import com.tchalanet.server.core.terminal.domain.model.TerminalSyncState;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaEntity;
import com.tchalanet.server.core.terminal.infra.persistence.TerminalJpaRepository;
import com.tchalanet.server.core.terminal.infra.persistence.mapper.TerminalMapper;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaTerminalReaderAdapter implements TerminalReaderPort {

  private final TerminalJpaRepository jpaRepository;
  private final TerminalMapper mapper;

  @Override
  public Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId) {
    return jpaRepository
        .findByTenantIdAndId(tenantId.value(), terminalId.value())
        .map(mapper::toDomain);
  }

  @Override
  public List<Terminal> listByOutlet(
      TenantId tenantId, OutletId outletId, PageRequest pageRequest) {
    return jpaRepository
        .findAllByTenantIdAndOutletIdAndDeletedAtIsNull(
            tenantId.value(), outletId.value(), pageRequest)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Terminal> listByTenant(TenantId tenantId, PageRequest pageRequest) {
    return jpaRepository.findAllByTenantIdAndDeletedAtIsNull(tenantId.value(), pageRequest).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public TchPage<TerminalSummaryView> search(
      TerminalSearchCriteria c, TchPageRequest pageRequest) {
    String qNorm = c.q() == null ? null : c.q().trim().toLowerCase();
    List<TerminalSummaryView> filtered =
        jpaRepository.findAll().stream()
            .filter(e -> e.getDeletedAt() == null)
            .filter(
                e ->
                    qNorm == null
                        || (e.getLabel() != null && e.getLabel().toLowerCase().contains(qNorm))
                        || (e.getInventoryTag() != null
                            && e.getInventoryTag().toLowerCase().contains(qNorm)))
            .filter(e -> c.outletId() == null || c.outletId().value().equals(e.getOutletId()))
            .filter(
                e ->
                    c.assignedUserId() == null
                        || (e.getAssignedUserId() != null
                            && c.assignedUserId().value().equals(e.getAssignedUserId())))
            .filter(e -> c.kind() == null || c.kind().name().equals(e.getKind()))
            .filter(e -> c.state() == null || c.state().name().equals(e.getState()))
            .filter(e -> c.syncState() == null || c.syncState().name().equals(e.getSyncState()))
            .filter(e -> c.activeForUser() == null || c.activeForUser() == e.isActiveForUser())
            .map(mapper::toDomain)
            .map(TerminalSummaryView::from)
            .toList();

    Pageable pageable = pageRequest.pageable();
    int total = filtered.size();
    int size = pageable.getPageSize();
    int pageNumber = pageable.getPageNumber();
    int from = (int) Math.min((long) pageNumber * size, total);
    int to = Math.min(from + size, total);
    int totalPages = size == 0 ? 0 : (int) Math.ceil((double) total / size);
    boolean last = pageNumber + 1 >= totalPages;
    return TchPage.of(
        filtered.subList(from, to),
        pageNumber,
        size,
        total,
        totalPages,
        last,
        !last,
        pageNumber > 0);
  }

  @Override
  public List<TerminalSummaryView> listOffline() {
    return listBySyncState(TerminalSyncState.OFFLINE);
  }

  @Override
  public List<TerminalSummaryView> listSyncPending() {
    return listBySyncState(TerminalSyncState.SYNC_PENDING);
  }

  private List<TerminalSummaryView> listBySyncState(TerminalSyncState target) {
    return jpaRepository.findAll().stream()
        .filter(e -> e.getDeletedAt() == null)
        .filter(e -> target.name().equals(e.getSyncState()))
        .map(mapper::toDomain)
        .map(TerminalSummaryView::from)
        .toList();
  }

  @Override
  public Optional<Terminal> findActiveForUser(UserId userId) {
    return jpaRepository.findAll().stream()
        .filter(e -> e.getDeletedAt() == null)
        .filter(e -> userId.value().equals(e.getAssignedUserId()))
        .filter(TerminalJpaEntity::isActiveForUser)
        .findFirst()
        .map(mapper::toDomain);
  }
}
