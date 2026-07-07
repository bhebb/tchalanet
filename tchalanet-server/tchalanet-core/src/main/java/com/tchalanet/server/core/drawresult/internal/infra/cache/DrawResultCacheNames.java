package com.tchalanet.server.core.drawresult.internal.infra.cache;

/**
 * Cache names for core/drawresult reads.
 *
 * <p>A published draw result is immutable; the three write paths (manual upsert, confirm, override)
 * all evict these caches after commit via {@link DrawResultCacheEvictor}. TTLs are kept short
 * (settlement-adjacent path) — eviction is the real freshness mechanism, the TTL is a tight safety
 * net.
 */
public final class DrawResultCacheNames {

  private DrawResultCacheNames() {}

  /** DrawResultView keyed by result id. */
  public static final String BY_ID = "core.drawresult.by_id";

  /** DrawResultId keyed by (resultSlotId, occurredAt). */
  public static final String ID_BY_SLOT_OCCURRED = "core.drawresult.id.by_slot_occurred";
}
