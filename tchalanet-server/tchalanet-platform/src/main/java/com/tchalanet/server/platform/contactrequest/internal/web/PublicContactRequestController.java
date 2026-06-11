package com.tchalanet.server.platform.contactrequest.internal.web;

import com.tchalanet.server.common.web.api.ApiNotice;
import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSubmittedView;
import com.tchalanet.server.platform.contactrequest.api.model.SubmitContactRequestCommand;
import com.tchalanet.server.platform.contactrequest.internal.service.ContactRequestSubmissionService;
import com.tchalanet.server.platform.contactrequest.internal.web.model.SubmitContactRequestHttpRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/public/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Public • Contact Requests")
public class PublicContactRequestController {

    private final ContactRequestSubmissionService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContactRequestSubmittedView> submit(
        @Valid @RequestBody SubmitContactRequestHttpRequest request
    ) {
        var result = service.submitWithNotification(toCommand(request));

        if (result.notificationFailed()) {
            return ApiResponse.warn(
                result.view(),
                ApiNotice.warn(
                    "CONTACT_NOTIFICATION_FAILED",
                    "La demande a été reçue, mais la notification interne n'a pas pu être envoyée."));
        }
        return ApiResponse.success(result.view());
    }

    private static SubmitContactRequestCommand toCommand(SubmitContactRequestHttpRequest req) {
        return new SubmitContactRequestCommand(
            req.intent(),
            req.fullName(),
            req.phone(),
            req.email(),
            req.organizationName(),
            req.city(),
            req.country(),
            req.outletCount(),
            req.preferredContactTime(),
            req.message(),
            req.consentToContact(),
            req.sourcePage());
    }
}
