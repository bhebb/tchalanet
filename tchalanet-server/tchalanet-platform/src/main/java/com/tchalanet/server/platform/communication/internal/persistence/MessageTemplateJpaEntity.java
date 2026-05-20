package com.tchalanet.server.platform.communication.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import com.tchalanet.server.platform.communication.api.model.value.CommunicationChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "message_template",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_message_template__scope",
        columnNames = {"tenant_id", "template_key", "channel", "locale"}))
@Getter
@Setter
public class MessageTemplateJpaEntity extends BaseEntity {

  @Column(name = "tenant_id", columnDefinition = "uuid")
  private UUID tenantId;

  @Column(name = "template_key", nullable = false, length = 120)
  private String templateKey;

  @Enumerated(EnumType.STRING)
  @Column(name = "channel", nullable = false, length = 32)
  private CommunicationChannel channel;

  @Column(name = "locale", nullable = false, length = 20)
  private String locale;

  @Column(name = "subject_template", columnDefinition = "text")
  private String subjectTemplate;

  @Column(name = "body_template", nullable = false, columnDefinition = "text")
  private String bodyTemplate;

  @Column(name = "active", nullable = false)
  private boolean active = true;
}
