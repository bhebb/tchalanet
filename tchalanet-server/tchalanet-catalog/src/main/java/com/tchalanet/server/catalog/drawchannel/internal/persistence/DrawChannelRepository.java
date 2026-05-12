package com.tchalanet.server.catalog.drawchannel.internal.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface DrawChannelRepository extends JpaRepository<DrawChannelEntity, UUID>, JpaSpecificationExecutor<DrawChannelEntity> {

  List<DrawChannelEntity> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

  List<DrawChannelEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNull(UUID tenantId);

  List<DrawChannelEntity> findByTenantIdAndActiveTrueAndDeletedAtIsNullOrderBySortOrderAsc(UUID tenantId);

  Optional<DrawChannelEntity> findByIdAndDeletedAtIsNull(UUID id);

  // Find channels for tenant by result_slot_id
  List<DrawChannelEntity> findByTenantIdAndResultSlotIdAndDeletedAtIsNull(UUID tenantId, UUID resultSlotId);

  // Find channels for tenant by result slot provider + slot_key (join with result_slot)
  @Query(value = """
      SELECT dc.* FROM draw_channel dc
      JOIN result_slot rs ON rs.id = dc.result_slot_id
      WHERE dc.tenant_id = ?1 AND rs.provider = ?2 AND rs.slot_key = ?3
        AND dc.deleted_at IS NULL AND rs.deleted_at IS NULL
      """, nativeQuery = true)
  List<DrawChannelEntity> findByTenantIdAndResultSlotProviderAndKey(UUID tenantId, String provider, String slotKey);

  // Repository helper: find by tenant + code (case-insensitive)
  Optional<DrawChannelEntity> findFirstByTenantIdAndCodeIgnoreCaseAndDeletedAtIsNull(UUID tenantId, String code);

    List<DrawChannelEntity> findByActiveTrueOrderBySortOrderAsc();

    List<DrawChannelEntity> findAllByOrderBySortOrderAsc();

    Optional<DrawChannelEntity> findFirstByCodeIgnoreCase(String trim);
}
