package com.tchalanet.server.catalog.address.internal.persistence;

import com.tchalanet.server.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

@Entity
@Table(name = "address")
@Getter
@Setter
@Audited
public class AddressJpaEntity extends BaseEntity {

    @Column(name = "line1", nullable = false, length = 255)
    private String line1;

    @Column(name = "line2", length = 255)
    private String line2;

    @Column(name = "city", nullable = false, length = 128)
    private String city;

    @Column(name = "region")
    private String regionOrProvinceOrState;

    @Column(name = "postal_code", length = 32)
    private String postalCode;

    @Column(name = "country", length = 64)
    private String country;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}
