package com.tchalanet.server.core.sales.infra.persistence.repository;

import com.tchalanet.server.core.sales.application.query.model.ListTicketsQuery.TicketFilter;
import com.tchalanet.server.core.sales.infra.persistence.TicketJpaEntity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

  private TicketSpecifications() {}

  public static Specification<TicketJpaEntity> fromFilter(TicketFilter filter) {
    return (root, query, cb) -> {
      var predicates = new ArrayList<Predicate>();

      if (filter.terminalId() != null) {
        predicates.add(cb.equal(root.get("id"), filter.terminalId().value()));
      }
      if (filter.drawId() != null) {
        predicates.add(cb.equal(root.get("drawId"), filter.drawId().value()));
      }
      if (filter.status() != null) {
        predicates.add(cb.equal(root.get("resultStatus"), filter.status()));
      }
      if (filter.from() != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), filter.from()));
      }
      if (filter.to() != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), filter.to()));
      }

      predicates.add(cb.isNull(root.get("deletedAt")));
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
