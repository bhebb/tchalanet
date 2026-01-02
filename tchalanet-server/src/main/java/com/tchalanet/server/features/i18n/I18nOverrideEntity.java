package com.tchalanet.server.features.i18n;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "i18n_override")
@Audited
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class I18nOverrideEntity extends BaseEntity {

  @Column(name = "tenant_id", nullable = false)
  private UUID tenantId;

  @Column(name = "locale", nullable = false)
  private String locale;

  @Column(name = "i18n_key", nullable = false)
  private String i18nKey;

  @Column(name = "i18n_value", nullable = false)
  private String i18nValue;
}
