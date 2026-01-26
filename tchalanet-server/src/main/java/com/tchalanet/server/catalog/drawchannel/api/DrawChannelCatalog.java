package com.tchalanet.server.catalog.drawchannel.api;

import com.tchalanet.server.catalog.drawchannel.api.model.*;
import com.tchalanet.server.common.types.id.DrawChannelId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;

import java.util.List;

/**
 * Catalog API for Draw Channels (tenant-scoped read/query API).
 *
 * This interface defines the public catalog operations (read/query) related to draw channels
 * exposed to other modules. It uses the project's typed ID wrappers (e.g. {@link DrawChannelId},
 * {@link TenantId}) rather than raw UUIDs/Strings to provide stronger typing across layers.
 *
 * Important contracts:
 * - This catalog is tenant-scoped: callers must provide a non-null {@link TenantId} for tenant
 *   oriented queries. Platform-wide (cross-tenant) queries belong to a distinct API (e.g.
 *   platform search) and should not be performed through this interface.
 * - The catalog is read-only: it returns Views/DTOs and never JPA entities.
 */
public interface DrawChannelCatalog {

  /**
   * List all draw channel summaries for the given tenant.
   *
   * @param tenantId tenant scope to list channels for (must not be null)
   * @param activeOnly when non-null and true, only return channels marked active; when null or
   *     false return all channels (including inactive). Implementations may also exclude
   *     soft-deleted channels.
   * @return non-null List of {@link DrawChannelSummaryView} (may be empty).
   */
  List<DrawChannelSummaryView> listAll(TenantId tenantId, Boolean activeOnly);

  /**
   * Find a single draw channel by its typed id within the tenant scope.
   *
   * Contract:
   * - tenantId and id must be non-null.
   * - returns Optional.empty() when the channel does not exist or is soft-deleted or does not
   *   belong to the tenant.
   *
   * @param tenantId tenant scope (must not be null)
   * @param id typed {@link DrawChannelId} (must not be null)
   * @return Optional containing {@link DrawChannelView} when found, otherwise Optional.empty().
   */
  java.util.Optional<DrawChannelView> findById(TenantId tenantId, DrawChannelId id);

  /**
   * Find a draw channel by its unique code within the tenant scope.
   *
   * Notes:
   * - Code lookup is tenant-scoped and should match exactly.
   * - Returns Optional.empty() if no channel matches or if the channel is deleted.
   *
   * @param tenantId tenant scope (must not be null)
   * @param code channel business code (case-sensitive unless implementation states otherwise)
   * @return Optional containing {@link DrawChannelView} when found, otherwise Optional.empty().
   */
  java.util.Optional<DrawChannelView> findByTenantAndCode(TenantId tenantId, String code);

  /**
   * List games configured for a given draw channel.
   *
   * @param tenantId tenant scope (must not be null)
   * @param channelId typed {@link DrawChannelId} (must not be null)
   * @return List of {@link DrawChannelGameView} describing per-game settings for that
   *     channel (may be empty).
   */
  List<DrawChannelGameView> listGamesByChannel(TenantId tenantId, DrawChannelId channelId);

  /**
   * List games grouped by channel (explicit typed result) for the tenant.
   *
   * Replaces the previous ambiguous Map-based API. Returns a stable list where each element
   * contains the channelCode and the list of games attached to that channel.
   *
   * @param tenantId tenant scope (must not be null)
   * @return non-null list (may be empty) of {@link ChannelGamesView}.
   */
  List<ChannelGamesView> listChannelGames(TenantId tenantId);

  /**
   * List calendar rows for channels with optional filtering.
   *
   * The method is explicit about which filters are applied. Implementations should apply the
   * activeOnly filter to the channel-level "active" toggle and the enabledOnly filter to the
   * per-channel-game "enabled" associations when constructing calendar rows.
   *
   * @param tenantId tenant scope (must not be null)
   * @param activeOnly when true, only include channels with channel.active == true; when null,
   *     do not filter by channel active flag.
   * @param enabledOnly when true, only include calendar rows that have at least one enabled
   *     game association (or reflect enabled semantics per implementation); when null, do not
   *     filter by enabled association.
   * @return list of {@link DrawChannelCalendarRow} (may be empty)
   */
  List<DrawChannelCalendarRow> listCalendarRows(TenantId tenantId, Boolean activeOnly, Boolean enabledOnly);

  /**
   * Perform a paged search for draw channels matching the provided criteria.
   *
   * Use {@link DrawChannelSearchCriteria} to express tenant-scoped or platform-scoped search;
   * when using this catalog (tenant-scoped) pass a non-null tenantId in the criteria. For
   * platform/cross-tenant search use a platform API (not this tenant-scoped catalog) or a
   * dedicated searchPlatform method.
   *
   * @param criteria search criteria (tenantId typically provided inside the criteria)
   * @param pageReq paging + sorting information (must not be null)
   * @return paged result containing matching {@link DrawChannelView}
   */
  TchPage<DrawChannelView> search(DrawChannelSearchCriteria criteria, TchPageRequest pageReq);
}
