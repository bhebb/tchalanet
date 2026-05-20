package com.tchalanet.server.platform.audit.api;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.platform.audit.api.model.AuditEventView;
import com.tchalanet.server.platform.audit.api.model.PurgeOldAuditEventsResult;
import com.tchalanet.server.platform.audit.api.model.request.ListAuditEventsRequest;
import com.tchalanet.server.platform.audit.api.model.request.LogAuditEventRequest;
import com.tchalanet.server.platform.audit.api.model.request.PurgeOldAuditEventsRequest;

public interface AuditApi {

    void logAuditEvent(LogAuditEventRequest request);

    TchPage<AuditEventView> listAuditEvents(ListAuditEventsRequest request);

    PurgeOldAuditEventsResult purgeOldAuditEvents(PurgeOldAuditEventsRequest request);
}
