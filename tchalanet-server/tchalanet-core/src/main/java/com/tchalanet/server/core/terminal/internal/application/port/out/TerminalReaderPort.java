package com.tchalanet.server.core.terminal.internal.application.port.out;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.types.id.TerminalId;
import com.tchalanet.server.common.exception.TchNotFoundException;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.core.terminal.api.query.TerminalSearchCriteria;
import com.tchalanet.server.core.terminal.api.query.TerminalSummaryView;
import com.tchalanet.server.core.terminal.internal.domain.model.Terminal;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

public interface TerminalReaderPort {

    Optional<Terminal> findById(TenantId tenantId, TerminalId terminalId);

    Terminal getById(TenantId tenantId, TerminalId terminalId);

    List<Terminal> listByOutlet(TenantId tenantId, OutletId outletId, PageRequest pageRequest);

    List<Terminal> listByTenant(TenantId tenantId, PageRequest pageRequest);

    /**
     * Paginated search with filters (RLS-scoped).
     */
    TchPage<TerminalSummaryView> search(TerminalSearchCriteria criteria, TchPageRequest pageRequest);

    /**
     * Lists all terminals whose syncState is OFFLINE. RLS-scoped.
     */
    List<TerminalSummaryView> listOffline();

    /**
     * Lists all terminals whose syncState is SYNC_PENDING. RLS-scoped.
     */
    List<TerminalSummaryView> listSyncPending();

    /**
     * Returns the terminal currently active for the given user (if any), tenant-scoped via RLS.
     */
    Optional<Terminal> findCurrentForUser(UserId userId);

    default Terminal getRequired(TenantId tenantId, TerminalId terminalId) {
        // An unknown or cross-tenant (RLS-filtered) terminal must surface as a clean 404,
        // not a raw IllegalArgumentException that the error handler maps to 500.
        return findById(tenantId, terminalId)
            .orElseThrow(() -> new TchNotFoundException(terminalId.toString(), "Terminal not found: "));
    }

    int countActiveByTenant(TenantId tenantId);
}
