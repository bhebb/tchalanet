package com.tchalanet.server.platform.archive.api;

import com.tchalanet.server.platform.archive.api.model.ArchivedEntityView;
import com.tchalanet.server.platform.archive.api.model.ArchiveRunView;
import com.tchalanet.server.platform.archive.api.model.TriggerArchiveRunRequest;
import java.util.List;
import java.util.UUID;

/**
 * Public interface for archive orchestration and lookup.
 *
 * <p>Tenant/admin callers use the lookup methods to find archived entities.
 * Platform/SUPER_ADMIN callers use trigger and restore methods.
 */
public interface ArchiveApi {

  // ── Lookup (tenant/admin scope) ───────────────────────────────────────────

  ArchivedEntityView findArchivedTicket(UUID tenantId, UUID ticketId);

  ArchivedEntityView findArchivedTicketByPublicCode(UUID tenantId, String publicCode);

  ArchivedEntityView findArchivedPayout(UUID tenantId, UUID payoutId);

  List<ArchivedEntityView> findArchivedAuditRecords(UUID tenantId, String entityType, UUID entityId);

  // ── Archive run management (platform scope) ───────────────────────────────

  ArchiveRunView triggerRun(TriggerArchiveRunRequest request, UUID requestedBy);

  List<ArchiveRunView> listRuns(int limit);
}
