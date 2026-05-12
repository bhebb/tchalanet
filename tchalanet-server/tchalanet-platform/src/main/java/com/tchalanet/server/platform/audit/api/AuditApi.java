package com.tchalanet.server.platform.audit.api;

import com.tchalanet.server.platform.audit.api.model.ListAuditEventsQuery;
import com.tchalanet.server.platform.audit.api.model.LogAuditEventCommand;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsCommand;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import java.util.List;

public interface AuditApi {

    void logAuditEvent(LogAuditEventCommand request);
    List<Object> listAuditEvents(ListAuditEventsQuery request);
    PurgeOldAuditEventsResult purgeOldAuditEvents(PurgeOldAuditEventsCommand request);
}
