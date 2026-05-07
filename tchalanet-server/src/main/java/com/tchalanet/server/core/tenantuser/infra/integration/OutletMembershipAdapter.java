package com.tchalanet.server.core.tenantuser.infra.integration;

import com.tchalanet.server.common.types.id.OutletId;
import com.tchalanet.server.common.types.id.RoleId;
import com.tchalanet.server.common.types.id.UserId;
import com.tchalanet.server.core.outlet.application.port.out.OutletMembershipPort;
import com.tchalanet.server.core.outlet.application.port.out.OutletMembershipReaderPort;
import com.tchalanet.server.core.outlet.application.query.model.OutletUserView;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserReaderPort;
import com.tchalanet.server.core.tenantuser.application.port.out.TenantUserWriterPort;
import com.tchalanet.server.core.tenantuser.domain.model.TenantUserMembership;
import com.tchalanet.server.core.tenantuser.infra.persistence.TenantUserJpaEntity;
import com.tchalanet.server.core.tenantuser.infra.persistence.TenantUserJpaRepository;
import com.tchalanet.server.core.user.infra.persistence.AppUserJpaEntity;
import com.tchalanet.server.core.user.infra.persistence.JpaAppUserRepository;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Cross-domain adapter implementing both write ({@link OutletMembershipPort}) and read ({@link
 * OutletMembershipReaderPort}) sides of outlet membership.
 *
 * <p>Lives in {@code core.tenantuser} since it owns the {@code tenant_user.outlet_id} column.
 */
@Component
@RequiredArgsConstructor
public class OutletMembershipAdapter
    implements OutletMembershipPort, OutletMembershipReaderPort {

  private final TenantUserReaderPort reader;
  private final TenantUserWriterPort writer;
  private final TenantUserJpaRepository tenantUserRepo;
  private final JpaAppUserRepository appUserRepo;

  // ── Write side ────────────────────────────────────────────────────────────

  @Override
  public void assignUserToOutlet(OutletId outletId, UserId userId) {
    TenantUserMembership membership = requireMembership(userId);
    if (Objects.equals(outletId, membership.outletId())) {
      return;
    }
    membership.assignOutlet(outletId);
    writer.upsertMembership(membership);
  }

  @Override
  public void removeUserFromOutlet(OutletId outletId, UserId userId) {
    TenantUserMembership membership = requireMembership(userId);
    if (!Objects.equals(outletId, membership.outletId())) {
      return;
    }
    membership.assignOutlet(null);
    writer.upsertMembership(membership);
  }

  // ── Read side ─────────────────────────────────────────────────────────────

  @Override
  public List<OutletUserView> listUsersByOutlet(OutletId outletId) {
    List<TenantUserJpaEntity> memberships =
        tenantUserRepo.findByOutletIdAndDeletedAtIsNull(outletId.value());
    if (memberships.isEmpty()) return List.of();

    List<UUID> userIds = memberships.stream().map(TenantUserJpaEntity::getUserId).toList();
    Map<UUID, AppUserJpaEntity> usersById =
        appUserRepo.findAllById(userIds).stream()
            .collect(Collectors.toMap(AppUserJpaEntity::getId, Function.identity()));

    return memberships.stream()
        .map(
            m -> {
              AppUserJpaEntity u = usersById.get(m.getUserId());
              return new OutletUserView(
                  UserId.of(m.getUserId()),
                  outletId,
                  u == null ? null : u.getDisplayName(),
                  u == null ? null : u.getEmail(),
                  u == null ? null : u.getUsername(),
                  m.getStatus() == null ? null : m.getStatus().name(),
                  RoleId.nullableOf(m.getRoleId()));
            })
        .toList();
  }

  @Override
  public long countUsersByOutlet(OutletId outletId) {
    return tenantUserRepo.countByOutletIdAndDeletedAtIsNull(outletId.value());
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private TenantUserMembership requireMembership(UserId userId) {
    Optional<TenantUserMembership> m = reader.findByUserId(userId);
    return m.orElseThrow(
        () ->
            new IllegalStateException(
                "No tenant_user membership found for userId=" + userId.value()));
  }
}
