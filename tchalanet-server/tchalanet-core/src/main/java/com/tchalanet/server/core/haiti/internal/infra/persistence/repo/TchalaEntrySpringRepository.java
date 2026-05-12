package com.tchalanet.server.core.haiti.internal.infra.persistence.repo;

import com.tchalanet.server.core.haiti.infra.persistence.entity.TchalaEntryJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TchalaEntrySpringRepository extends JpaRepository<TchalaEntryJpaEntity, UUID> {

  @Query(
      """
    select e from TchalaEntryJpaEntity e
    where e.lang = :lang
      and e.dedupeKey = :dedupeKey
      and e.status = 'APPROVED'
      and e.canonicalEntryId is null
  """)
  Optional<TchalaEntryJpaEntity> findCanonicalApprovedByKey(
      @Param("lang") String lang, @Param("dedupeKey") String dedupeKey);

  @Query(
      """
    select e from TchalaEntryJpaEntity e
    where e.lang = :lang
      and e.status = 'PENDING'
      and (:conflictOnly = false or e.conflictWithEntryId is not null)
    order by e.createdAt desc
  """)
  Page<TchalaEntryJpaEntity> listPending(
      @Param("lang") String lang, @Param("conflictOnly") boolean conflictOnly, Pageable pageable);

    @Query(
        value =
            """
      select distinct e from TchalaEntryJpaEntity e
      join e.numbers n
      where e.lang = :lang
        and e.status = 'APPROVED'
        and e.canonicalEntryId is null
        and n.pk.lang = :lang
        and n.pk.number = :number
      order by e.updatedAt desc
    """,
        countQuery =
            """
      select count(distinct e.id) from TchalaEntryJpaEntity e
      join e.numbers n
      where e.lang = :lang
        and e.status = 'APPROVED'
        and e.canonicalEntryId is null
        and n.pk.lang = :lang
        and n.pk.number = :number
    """)
    Page<TchalaEntryJpaEntity> findApprovedByNumber(
        @Param("lang") String lang, @Param("number") short number, Pageable pageable);

  @Query(
      """
    select e from TchalaEntryJpaEntity e
    where e.lang = :lang
      and e.status = 'APPROVED'
      and e.canonicalEntryId is null
      and (
        lower(e.dream) like lower(concat('%', :text, '%'))
        or lower(e.note) like lower(concat('%', :text, '%'))
      )
    order by e.updatedAt desc
  """)
  Page<TchalaEntryJpaEntity> searchApproved(
      @Param("lang") String lang, @Param("text") String text, Pageable pageable);

  // Load a single entry with its numbers (fetch join) to avoid lazy loading issues
  @Query(
      "select distinct e from TchalaEntryJpaEntity e left join fetch e.numbers n where e.id = :id")
  Optional<TchalaEntryJpaEntity> findByIdWithNumbers(@Param("id") UUID id);
}
