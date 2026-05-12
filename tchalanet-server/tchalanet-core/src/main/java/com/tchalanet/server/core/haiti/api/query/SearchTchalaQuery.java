package com.tchalanet.server.core.haiti.api.query;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.paging.TchPage;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;

/** Query to search the Tchala catalog with pagination. */
public record SearchTchalaQuery(String lang, String text, int page, int size)
    implements Query<TchPage<TchalaEntry>> {}
