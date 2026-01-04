package com.tchalanet.server.core.address.infra.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "address")
@Getter
@Setter
public class AddressJpaEntity extends BaseEntity {

  @Column(name = "line1", nullable = false)
  private String line1;

  @Column(name = "line2")
  private String line2;

  @Column(name = "city", nullable = false)
  private String city;

  @Column(name = "region")
  private String region;

  @Column(name = "country", nullable = false)
  private String country;

  @Column(name = "postal_code")
  private String postalCode;

  @Column(name = "latitude")
  private Double latitude;

  @Column(name = "longitude")
  private Double longitude;
}
