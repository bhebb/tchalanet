package com.tchalanet.server.core.haiti.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.core.haiti.domain.tchala.model.TchalaEntry;

/** Query to get Tchala entries containing a given number with pagination. */
public record GetTchalaByNumberQuery(String lang, int number, int page, int size)
    implements Query<TchPage<TchalaEntry>> {}
