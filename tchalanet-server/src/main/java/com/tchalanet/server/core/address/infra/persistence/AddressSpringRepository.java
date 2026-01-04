package com.tchalanet.server.core.address.infra.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressSpringRepository extends JpaRepository<AddressJpaEntity, UUID> {
  Optional<AddressJpaEntity> findById(UUID id);

  // Deduplication helper: find an existing address matching postalCode, city and line1
  // (case-insensitive)
  Optional<AddressJpaEntity> findFirstByPostalCodeIgnoreCaseAndCityIgnoreCaseAndLine1IgnoreCase(
      String postalCode, String city, String line1);
}
