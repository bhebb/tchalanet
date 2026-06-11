package com.tchalanet.server.platform.contactrequest.internal.web;

import com.tchalanet.server.common.web.api.ApiResponse;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.common.web.paging.TchPaging;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestAdminDetailView;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;
import com.tchalanet.server.platform.contactrequest.internal.service.ContactRequestAdminService;
import com.tchalanet.server.platform.contactrequest.internal.web.model.UpdateContactNotesHttpRequest;
import com.tchalanet.server.platform.contactrequest.internal.web.model.UpdateContactStatusHttpRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/platform/contact-requests")
@RequiredArgsConstructor
@Tag(name = "Platform • Contact Requests")
public class PlatformContactRequestAdminController {

    private final ContactRequestAdminService service;

    @GetMapping
    public ApiResponse<TchPage<ContactRequestSummaryView>> list(
        @RequestParam(required = false) ContactRequestStatus status,
        @RequestParam(required = false) ContactRequestIntent intent,
        @TchPaging(
            allowedSort = {"createdAt", "status", "intent", "reference"},
            defaultSort = {"createdAt,desc"})
        TchPageRequest pageReq
    ) {
        return ApiResponse.success(service.list(status, intent, pageReq.pageable()));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContactRequestAdminDetailView> get(@PathVariable UUID id) {
        return ApiResponse.success(service.get(id));
    }

    @PatchMapping("/{id}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateContactStatusHttpRequest request
    ) {
        service.updateStatus(id, request.status());
    }

    @PatchMapping("/{id}/notes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateNotes(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateContactNotesHttpRequest request
    ) {
        service.updateNotes(id, request.internalNotes(), request.externalTool(), request.externalReference());
    }
}
