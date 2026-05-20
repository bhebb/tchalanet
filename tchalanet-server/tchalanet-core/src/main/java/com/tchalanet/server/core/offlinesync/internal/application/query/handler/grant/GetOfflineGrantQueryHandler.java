package com.tchalanet.server.core.offlinesync.internal.application.query.handler.grant;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.grant.GetOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.query.mapper.OfflineGrantViewMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineGrantQueryHandler
    implements QueryHandler<GetOfflineGrantQuery, OfflineGrantView> {

    private final OfflineGrantReaderPort grantReader;

    @Override
    @TchTx(readOnly = true)
    public OfflineGrantView handle(GetOfflineGrantQuery query) {
        return OfflineGrantViewMapper.toView(grantReader.getRequired(query.grantId()));
    }
}
