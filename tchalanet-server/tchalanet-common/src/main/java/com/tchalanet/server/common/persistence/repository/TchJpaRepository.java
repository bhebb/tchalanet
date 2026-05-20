package com.tchalanet.server.common.persistence.repository;

import com.tchalanet.server.common.persistence.AuditableEntity;
import com.tchalanet.server.common.web.error.ProblemRest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TchJpaRepository<T extends AuditableEntity, ID>
    extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    @Override
    default void delete(T entity) {
        throw new UnsupportedOperationException("Hard delete is forbidden. Use SoftDeleteExecutor.softDelete.");
    }

    @Override
    default void deleteById(ID id) {
        throw new UnsupportedOperationException("Hard delete is forbidden. Use SoftDeleteExecutor.softDeleteById.");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Hard delete is forbidden. Use SoftDeleteExecutor.softDelete.");
    }

    @Override
    default void deleteAll(Iterable<? extends T> entities) {
        throw new UnsupportedOperationException("Hard delete is forbidden. Use SoftDeleteExecutor.softDelete.");
    }

    default T getRequired(ID id) {
        return findById(id)
            .orElseThrow(() -> ProblemRest.notFound("entity.not_found", id));
    }
}
