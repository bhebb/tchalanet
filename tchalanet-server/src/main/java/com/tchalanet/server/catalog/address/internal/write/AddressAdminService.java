package com.tchalanet.server.catalog.address.internal.write;

import com.tchalanet.server.catalog.address.api.AddressView;
import com.tchalanet.server.catalog.address.internal.cache.AddressCacheNames;
import com.tchalanet.server.catalog.address.internal.mapper.AddressMapper;
import com.tchalanet.server.catalog.address.internal.persistence.AddressJpaEntity;
import com.tchalanet.server.catalog.address.internal.persistence.AddressJpaRepository;
import com.tchalanet.server.catalog.address.internal.web.model.BaseAddressRequest;
import com.tchalanet.server.catalog.address.internal.web.model.CreateAddressRequest;
import com.tchalanet.server.catalog.address.internal.web.model.UpdateAddressRequest;
import com.tchalanet.server.common.error.ProblemRest;
import com.tchalanet.server.common.types.id.AddressId;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressAdminService {

    private final AddressJpaRepository repo;
    private final Clock clock;
    private final AddressMapper mapper;

    @Transactional
    @CacheEvict(cacheNames = {AddressCacheNames.ACTIVE, AddressCacheNames.BY_ID}, allEntries = true)
    public AddressView create(CreateAddressRequest req) {
        var e = new AddressJpaEntity();
        apply(req, e);
        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {AddressCacheNames.ACTIVE, AddressCacheNames.BY_ID}, allEntries = true)
    public AddressView update(AddressId id, UpdateAddressRequest req) {
        var e =
            repo.findByIdAndDeletedAtIsNull(id.value())
                .orElseThrow(() -> ProblemRest.notFound("address_not_found" + id));

        var saved = repo.save(e);
        return mapper.toView(saved);
    }

    @Transactional
    @CacheEvict(cacheNames = {AddressCacheNames.ACTIVE, AddressCacheNames.BY_ID}, allEntries = true)
    public void softDelete(AddressId id) {
        var e =
            repo.findByIdAndDeletedAtIsNull(id.value())
                .orElseThrow(() -> ProblemRest.notFound("address_not_found" + id));

        e.setDeletedAt(Instant.now(clock));
        repo.save(e);
    }

    @Transactional(readOnly = true)
    public List<AddressJpaEntity> findExactMatches(
        String postalCode, String line1, String city, String country) {
        return repo
            .findByDeletedAtIsNullAndPostalCodeIgnoreCaseAndLine1IgnoreCaseAndCityIgnoreCaseAndCountryIgnoreCase(
                postalCode, line1, city, country);
    }

    /**
     * Dedup = keep one, soft-delete the rest. No hard-delete by default.
     * This avoids breaking references unexpectedly.
     */
    @Transactional
    @CacheEvict(cacheNames = {AddressCacheNames.ACTIVE, AddressCacheNames.BY_ID}, allEntries = true)
    public List<AddressView> dedupByFields(
        String postalCode, String line1, String city, String country) {

        var matches = findExactMatches(postalCode, line1, city, country);
        if (matches.size() <= 1) return mapper.toViews(matches);

        // keep the "oldest" (or choose rule: lowest id / earliest createdAt)
        matches.sort(Comparator.comparing(AddressJpaEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        var keep = matches.get(0);

        for (int i = 1; i < matches.size(); i++) {
            var dup = matches.get(i);
            dup.setDeletedAt(Instant.now(clock));
            keep = repo.save(dup);
        }

        return List.of(mapper.toView(keep));
    }

    private static void apply(BaseAddressRequest req, AddressJpaEntity e) {
        if (req == null) return;

        if (req.line1() != null) e.setLine1(req.line1().trim());
        if (req.line2() != null) e.setLine2(req.line2().trim());
        if (req.city() != null) e.setCity(req.city().trim());
        if (req.postalCode() != null) e.setPostalCode(req.postalCode().trim());
        if (req.country() != null) e.setCountry(req.country().trim());
        if (req.active() != null) e.setActive(req.active());
    }
}
