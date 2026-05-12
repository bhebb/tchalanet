package com.tchalanet.server.core.haiti.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;

/** Query to list pending entries with pagination. */
public record ListPendingTchalaEntriesQuery(String lang, boolean conflictOnly, int page, int size)
    implements Query<TchPage<TchalaEntry>> {}
