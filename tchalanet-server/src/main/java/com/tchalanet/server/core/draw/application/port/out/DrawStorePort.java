package com.tchalanet.server.core.draw.application.port.out;

import com.tchalanet.server.core.draw.application.query.projection.DueToCloseRow;
import com.tchalanet.server.core.draw.application.query.projection.NewDrawRow;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DrawStorePort {
    boolean exists(UUID tenantId, UUID drawChannelId, Instant scheduledAt);

    int bulkInsert(List<NewDrawRow> rows);

    List<DueToCloseRow> findDueToClose(Instant now, int limit);

    int bulkClose(List<UUID> drawIds);
}
