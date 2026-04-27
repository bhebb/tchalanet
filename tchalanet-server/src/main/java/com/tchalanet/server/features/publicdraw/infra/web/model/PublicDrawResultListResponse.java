package com.tchalanet.server.features.publicdraw.infra.web.model;

import com.tchalanet.server.common.web.paging.TchPage;
import java.util.List;

public record PublicDrawResultListResponse(
    TchPage<PublicDrawResultItemResponse> page, List<PublicNextDrawItem> nextDraws) {}
