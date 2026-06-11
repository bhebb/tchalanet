package com.tchalanet.server.platform.contactrequest.internal.mapper;

import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestAdminDetailView;
import com.tchalanet.server.platform.contactrequest.api.model.ContactRequestSummaryView;
import com.tchalanet.server.platform.contactrequest.internal.persistence.ContactRequestJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class ContactRequestMapper {

    public ContactRequestSummaryView toSummaryView(ContactRequestJpaEntity entity) {
        return new ContactRequestSummaryView(
            entity.getId(),
            entity.getReference(),
            entity.getIntent(),
            entity.getFullName(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getCity(),
            entity.getCountry(),
            entity.getStatus(),
            entity.getCreatedAt());
    }

    public ContactRequestAdminDetailView toDetailView(ContactRequestJpaEntity entity) {
        return new ContactRequestAdminDetailView(
            entity.getId(),
            entity.getReference(),
            entity.getIntent(),
            entity.getFullName(),
            entity.getPhone(),
            entity.getEmail(),
            entity.getOrganizationName(),
            entity.getCity(),
            entity.getCountry(),
            entity.getOutletCount(),
            entity.getPreferredContactTime(),
            entity.getMessage(),
            entity.isConsentToContact(),
            entity.getStatus(),
            entity.getInternalNotes(),
            entity.getExternalTool(),
            entity.getExternalReference(),
            entity.getExportedAt(),
            entity.getSourcePage(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }
}
