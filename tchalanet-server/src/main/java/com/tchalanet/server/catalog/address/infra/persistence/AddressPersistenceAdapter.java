package com.tchalanet.server.catalog.address.infra.persistence;

import com.tchalanet.server.catalog.address.application.port.out.AddressReaderPort;
import com.tchalanet.server.catalog.address.application.port.out.AddressWriterPort;
import com.tchalanet.server.catalog.address.domain.model.Address;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AddressPersistenceAdapter implements AddressReaderPort, AddressWriterPort {

  private final AddressSpringRepository repo;

  @Override
  public Optional<Address> findById(UUID id) {
    return repo.findById(id)
        .map(
            e ->
                new Address(
                    e.getId(),
                    e.getLine1(),
                    e.getLine2(),
                    e.getCity(),
                    e.getRegion(),
                    e.getCountry(),
                    e.getPostalCode(),
                    e.getLatitude(),
                    e.getLongitude()));
  }

  @Override
  public UUID save(Address address) {
    // Validation: require line1, city, country
    if (!StringUtils.hasText(address.line1()))
      throw new IllegalArgumentException("address.line1 is required");
    if (!StringUtils.hasText(address.city()))
      throw new IllegalArgumentException("address.city is required");
    if (!StringUtils.hasText(address.country()))
      throw new IllegalArgumentException("address.country is required");

    AddressJpaEntity entity = null;

    // If id provided, try to update existing
    if (address.id() != null) {
      entity = repo.findById(address.id()).orElse(null);
    }

    // Deduplication: search by postalCode + city + line1 (ignore case)
    if (entity == null && StringUtils.hasText(address.postalCode())) {
      entity =
          repo.findFirstByPostalCodeIgnoreCaseAndCityIgnoreCaseAndLine1IgnoreCase(
                  address.postalCode(), address.city(), address.line1())
              .orElse(null);
    }

    if (entity == null) entity = new AddressJpaEntity();

    entity.setLine1(address.line1());
    entity.setLine2(address.line2());
    entity.setCity(address.city());
    entity.setRegion(address.region());
    entity.setCountry(address.country());
    entity.setPostalCode(address.postalCode());
    entity.setLatitude(address.latitude());
    entity.setLongitude(address.longitude());

    AddressJpaEntity saved = repo.save(entity);
    return saved.getId();
  }
}
