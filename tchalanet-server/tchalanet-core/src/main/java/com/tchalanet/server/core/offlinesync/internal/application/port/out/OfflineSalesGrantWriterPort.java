package com.tchalanet.server.core.offlinesync.internal.application.port.out;

import com.tchalanet.server.core.offlinesync.internal.domain.model.OfflineSalesGrant;

public interface OfflineSalesGrantWriterPort {
    OfflineSalesGrant save(OfflineSalesGrant grant);
}
