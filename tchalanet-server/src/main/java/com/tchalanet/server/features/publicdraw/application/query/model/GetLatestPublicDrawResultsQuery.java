package com.tchalanet.server.features.publicdraw.application.query.model;

import com.tchalanet.server.common.bus.Query;
import com.tchalanet.server.features.publicdraw.infra.web.model.PublicLatestDrawResultsResponse;
import java.util.List;

public record GetLatestPublicDrawResultsQuery(int limitPerChannel)
    implements Query<List<PublicLatestDrawResultsResponse>> {}
