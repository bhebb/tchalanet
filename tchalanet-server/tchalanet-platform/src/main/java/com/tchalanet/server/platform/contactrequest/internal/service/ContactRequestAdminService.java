package com.tchalanet.server.platform.contactrequest.internal.service;

import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageMapper;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestAdminApi;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestAdminDetailView;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;
import com.tchalanet.server.platform.contactrequest.internal.mapper.ContactRequestMapper;
import com.tchalanet.server.platform.contactrequest.internal.persistence.ContactRequestJpaRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ContactRequestAdminService implements ContactRequestAdminApi {

    private final ContactRequestJpaRepository repository;
    private final ContactRequestMapper mapper;

    public TchPage<ContactRequestSummaryView> list(
        ContactRequestStatus status,
        ContactRequestIntent intent,
        TchPageRequest pageRequest
    ) {
        return list(status, intent, pageRequest.pageable());
    }

    public TchPage<ContactRequestSummaryView> list(
        ContactRequestStatus status,
        ContactRequestIntent intent,
        Pageable pageable
    ) {
        return TchPageMapper.map(
            repository.search(status, intent, pageable),
            mapper::toSummaryView);
    }

    public ContactRequestAdminDetailView get(UUID id) {
        return repository.findById(id)
            .map(mapper::toDetailView)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact request not found: " + id));
    }

    @Transactional
    public void updateStatus(UUID id, ContactRequestStatus newStatus) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact request not found: " + id));
        entity.setStatus(newStatus);
    }

    @Transactional
    public void updateNotes(UUID id, String internalNotes, String externalTool, String externalReference) {
        var entity = repository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contact request not found: " + id));
        entity.setInternalNotes(internalNotes);
        entity.setExternalTool(externalTool);
        entity.setExternalReference(externalReference);
    }
}
