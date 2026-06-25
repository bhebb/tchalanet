package com.tchalanet.server.platform.contactrequest.api;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;

public interface ContactRequestAdminApi {

    TchPage<ContactRequestSummaryView> list(
        ContactRequestStatus status,
        ContactRequestIntent intent,
        TchPageRequest pageRequest
    );
}
