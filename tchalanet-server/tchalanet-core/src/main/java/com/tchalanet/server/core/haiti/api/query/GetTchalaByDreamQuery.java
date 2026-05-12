package com.tchalanet.server.core.haiti.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.core.haiti.internal.domain.tchala.model.TchalaEntry;
import java.util.Optional;

/**
 * Query to get the canonical approved Tchala entry by dream text (lang + dream normalized slotKey).
 */
public record GetTchalaByDreamQuery(String lang, String dream)
    implements Query<Optional<TchalaEntry>> {}
