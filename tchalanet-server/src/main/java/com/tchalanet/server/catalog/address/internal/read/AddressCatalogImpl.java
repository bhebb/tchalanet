package com.tchalanet.server.catalog.address.internal.read;

import com.tchalanet.server.catalog.address.api.AddressCatalog;
import com.tchalanet.server.catalog.address.api.AddressSearchCriteria;
import com.tchalanet.server.catalog.address.api.AddressView;
import com.tchalanet.server.catalog.address.internal.cache.AddressCacheNames;
import com.tchalanet.server.catalog.address.internal.mapper.AddressMapper;
import com.tchalanet.server.catalog.address.internal.persistence.AddressJpaEntity;
import com.tchalanet.server.catalog.address.internal.persistence.AddressJpaRepository;
import com.tchalanet.server.common.types.id.AddressId;
import com.tchalanet.server.common.web.paging.TchPage;
import com.tchalanet.server.common.web.paging.TchPageRequest;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AddressCatalogImpl implements AddressCatalog {

    private final AddressJpaRepository repo;
    private final AddressMapper addressMapper;

    @Override
    @Cacheable(cacheNames = AddressCacheNames.ACTIVE)
    public List<AddressView> listActive() {
        var entities = repo.findByActiveTrueAndDeletedAtIsNullOrderByUpdatedAtDesc();
        return addressMapper.toViews(entities);
    }

    @Override
    @Cacheable(cacheNames = AddressCacheNames.BY_ID, key = "#id == null ? '' : #id.value()")
    public Optional<AddressView> findById(AddressId id) {
        if (id == null) return Optional.empty();
        return repo.findByIdAndDeletedAtIsNull(id.value()).map(addressMapper::toView);
    }

    @Override
    public TchPage<AddressView> search(AddressSearchCriteria criteria, TchPageRequest pageReq) {
        var safeCriteria = (criteria == null) ? AddressSearchCriteria.empty() : criteria;

        Specification<AddressJpaEntity> spec =
            (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.isNull(root.get("deletedAt")));

                if (safeCriteria.line1() != null && !safeCriteria.line1().isBlank()) {
                    predicates.add(
                        cb.like(
                            cb.lower(root.get("line1")),
                            "%" + safeCriteria.line1().toLowerCase() + "%"));
                }
                if (safeCriteria.city() != null && !safeCriteria.city().isBlank()) {
                    predicates.add(
                        cb.equal(cb.lower(root.get("city")), safeCriteria.city().toLowerCase()));
                }
                if (safeCriteria.postalCode() != null && !safeCriteria.postalCode().isBlank()) {
                    predicates.add(
                        cb.equal(
                            cb.lower(root.get("postalCode")), safeCriteria.postalCode().toLowerCase()));
                }
                if (safeCriteria.country() != null && !safeCriteria.country().isBlank()) {
                    predicates.add(
                        cb.equal(cb.lower(root.get("country")), safeCriteria.country().toLowerCase()));
                }
                if (safeCriteria.outletCode() != null && !safeCriteria.outletCode().isBlank()) {
                    predicates.add(
                        cb.equal(
                            cb.lower(root.get("outletCode")), safeCriteria.outletCode().toLowerCase()));
                }
                if (safeCriteria.active() != null) {
                    predicates.add(cb.equal(root.get("active"), safeCriteria.active()));
                }

                return cb.and(predicates.toArray(new Predicate[0]));
            };

        Page<AddressJpaEntity> page = repo.findAll(spec, pageReq.pageable());
        var items = page.getContent().stream().map(addressMapper::toView).toList();

        return TchPage.of(
            items,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isLast(),
            page.hasNext(),
            page.hasPrevious());
    }
}
