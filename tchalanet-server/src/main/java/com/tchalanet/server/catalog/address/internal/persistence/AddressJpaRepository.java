package com.tchalanet.server.catalog.address.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AddressJpaRepository
    extends JpaRepository<AddressJpaEntity, UUID>, JpaSpecificationExecutor<AddressJpaEntity> {

    List<AddressJpaEntity> findByActiveTrueAndDeletedAtIsNullOrderByUpdatedAtDesc();

    Optional<AddressJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    // Exact match helper for dedup (case-insensitive) - adjust fields as needed
    List<AddressJpaEntity>
    findByDeletedAtIsNullAndPostalCodeIgnoreCaseAndLine1IgnoreCaseAndCityIgnoreCaseAndCountryIgnoreCase(
        String postalCode, String line1, String city, String country);
}
