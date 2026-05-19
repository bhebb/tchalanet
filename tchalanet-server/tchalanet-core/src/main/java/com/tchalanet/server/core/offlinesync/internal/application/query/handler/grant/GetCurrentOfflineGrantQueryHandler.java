package com.tchalanet.server.core.offlinesync.internal.application.query.handler.grant;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.grant.GetCurrentOfflineGrantQuery;
import com.tchalanet.server.core.offlinesync.api.query.grant.OfflineGrantView;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineGrantReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.query.mapper.OfflineGrantViewMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetCurrentOfflineGrantQueryHandler
    implements QueryHandler<GetCurrentOfflineGrantQuery, OfflineGrantView> {

    private final OfflineGrantReaderPort grantReader;

    @Override
    @TchTx(readOnly = true)
    public OfflineGrantView handle(GetCurrentOfflineGrantQuery query) {
        return grantReader
            .findCurrentActive(query.sellerUserId(), query.terminalId(), query.deviceId())
            .map(OfflineGrantViewMapper::toView)
            .orElse(null);
    }
}
