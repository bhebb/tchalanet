package com.tchalanet.server.core.offlinesync.internal.application.query.handler.syncbatch;

import com.tchalanet.server.common.bus.QueryHandler;
import com.tchalanet.server.common.stereotype.TchTx;
import com.tchalanet.server.common.stereotype.UseCase;
import com.tchalanet.server.core.offlinesync.api.query.syncbatch.GetOfflineSyncBatchQuery;
import com.tchalanet.server.core.offlinesync.api.query.syncbatch.OfflineSyncBatchView;
import com.tchalanet.server.core.offlinesync.internal.application.port.out.OfflineSyncBatchReaderPort;
import com.tchalanet.server.core.offlinesync.internal.application.query.mapper.OfflineSyncBatchViewMapper;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetOfflineSyncBatchQueryHandler
    implements QueryHandler<GetOfflineSyncBatchQuery, OfflineSyncBatchView> {

    private final OfflineSyncBatchReaderPort syncBatchReader;

    @Override
    @TchTx(readOnly = true)
    public OfflineSyncBatchView handle(GetOfflineSyncBatchQuery query) {
        return OfflineSyncBatchViewMapper.toView(syncBatchReader.getRequired(query.syncBatchId()));
    }
}
