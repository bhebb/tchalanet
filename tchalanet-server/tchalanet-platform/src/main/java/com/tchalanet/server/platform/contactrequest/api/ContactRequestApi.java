package com.tchalanet.server.platform.contactrequest.api;

import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSubmittedView;
import com.tchalanet.server.platform.contactrequest.api.model.SubmitContactRequestCommand;

/**
 * Platform API for contact request submission.
 * Admin CRUD is internal — consumed only by platform controllers.
 */
public interface ContactRequestApi {

    ContactRequestSubmittedView submit(SubmitContactRequestCommand command);
}
