package com.tchalanet.server.platform.contactrequest.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestIntent;
import com.tchalanet.server.platform.contactrequest.api.ContactRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "public_contact_request")
@Getter
@Setter
@NoArgsConstructor
public class ContactRequestJpaEntity extends BaseEntity {

    @Column(name = "reference", nullable = false, unique = true, length = 32)
    private String reference;

    @Column(name = "intent", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private ContactRequestIntent intent;

    @Column(name = "full_name", nullable = false, length = 160)
    private String fullName;

    @Column(name = "phone", nullable = false, length = 64)
    private String phone;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "organization_name", length = 180)
    private String organizationName;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "outlet_count")
    private Integer outletCount;

    @Column(name = "preferred_contact_time", length = 120)
    private String preferredContactTime;

    @Column(name = "message", nullable = false, columnDefinition = "text")
    private String message;

    @Column(name = "consent_to_contact", nullable = false)
    private boolean consentToContact;

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private ContactRequestStatus status;

    @Column(name = "internal_notes", columnDefinition = "text")
    private String internalNotes;

    @Column(name = "external_tool", length = 80)
    private String externalTool;

    @Column(name = "external_reference", length = 160)
    private String externalReference;

    @Column(name = "exported_at")
    private Instant exportedAt;

    @Column(name = "source_page", length = 160)
    private String sourcePage;

    public static ContactRequestJpaEntity create(
        UUID id,
        String reference,
        ContactRequestIntent intent,
        String fullName,
        String phone,
        String email,
        String organizationName,
        String city,
        String country,
        Integer outletCount,
        String preferredContactTime,
        String message,
        boolean consentToContact,
        String sourcePage
    ) {
        var entity = new ContactRequestJpaEntity();
        entity.setId(id);
        entity.reference = reference;
        entity.intent = intent;
        entity.fullName = fullName;
        entity.phone = phone;
        entity.email = email;
        entity.organizationName = organizationName;
        entity.city = city;
        entity.country = country;
        entity.outletCount = outletCount;
        entity.preferredContactTime = preferredContactTime;
        entity.message = message;
        entity.consentToContact = consentToContact;
        entity.sourcePage = sourcePage;
        entity.status = ContactRequestStatus.RECEIVED;
        return entity;
    }
}
