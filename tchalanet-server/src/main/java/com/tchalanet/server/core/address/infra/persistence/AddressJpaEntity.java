package com.tchalanet.server.core.address.infra.persistence;

import com.tchalanet.server.common.persistence.BaseTenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for address (tenant-scoped).
 * Includes normalized_key for deduplication with unique tenant-scoped index.
 * Per spec: RLS enabled at DB level.
 */
@Entity
@Table(
    name = "address",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_address_tenant_normalized_key",
        columnNames = {"tenant_id", "normalized_key"}
    )
)
@Getter
@Setter
public class AddressJpaEntity extends BaseTenantEntity {

  @Column(name = "line1", nullable = false, length = 256)
  private String line1;

  @Column(name = "line2", length = 256)
  private String line2;

  @Column(name = "city", nullable = false, length = 128)
  private String city;

  @Column(name = "region", length = 128)
  private String region;

  @Column(name = "country", nullable = false, length = 2)
  private String country;

  @Column(name = "postal_code", length = 16)
  private String postalCode;

  @Column(name = "normalized_key", nullable = false, length = 64)
  private String normalizedKey;

  @Column(name = "deleted", nullable = false)
  private boolean deleted = false;
}
