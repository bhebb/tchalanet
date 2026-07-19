package com.tchalanet.server.core.sellerterminal.internal.infra.identity;

import com.tchalanet.server.common.types.id.SellerTerminalId;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalIdentityProvisionPort;
import com.tchalanet.server.core.sellerterminal.internal.application.port.out.SellerTerminalReaderPort;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalExternalIdentityJpaEntity;
import com.tchalanet.server.core.sellerterminal.internal.infra.persistence.SellerTerminalExternalIdentityJpaRepository;
import com.tchalanet.server.platform.identity.api.IdentityUserNotFoundException;
import com.tchalanet.server.platform.identity.api.SellerTerminalIdentityProvisioningApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
class CoreSellerTerminalIdentityProvisionAdapter implements SellerTerminalIdentityProvisionPort {

  private final SellerTerminalIdentityProvisioningApi identityProvisioning;
  private final SellerTerminalExternalIdentityJpaRepository identityRepo;
  private final SellerTerminalReaderPort reader;

  @Override
  public void provision(
      SellerTerminalId id,
      TenantId tenantId,
      String terminalCode,
      String displayName,
      String initialPin) {
    if (identityRepo.existsBySellerTerminalId(id.value())) {
      log.info("SellerTerminal {} already has external identity - skipping", id.value());
      return;
    }
    var result =
        identityProvisioning.provisionSellerTerminal(id, terminalCode, displayName, initialPin);

    var entity = new SellerTerminalExternalIdentityJpaEntity();
    entity.setId(id.value());
    entity.setTenantId(tenantId.value());
    entity.setSellerTerminalId(id.value());
    entity.setProvider(result.provider().name());
    entity.setIssuer(result.issuer());
    entity.setExternalSubject(result.externalSubject());
    identityRepo.save(entity);

    log.info(
        "Provisioned external identity for SellerTerminal {} subject={}",
        id.value(),
        result.externalSubject());
  }

  @Override
  public boolean hasExternalIdentity(SellerTerminalId id) {
    return identityRepo.existsBySellerTerminalId(id.value());
  }

  @Override
  public void resetPin(SellerTerminalId id, TenantId tenantId, String newPin) {
    var identity =
        identityRepo
            .findBySellerTerminalId(id.value())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "No external identity found for SellerTerminal " + id.value()));
    try {
      identityProvisioning.resetPasswordForSubject(identity.getExternalSubject(), newPin);
    } catch (IdentityUserNotFoundException ex) {
      // The provider lost the account (emulator restart, identity drift): re-provision it
      // under the same subject so the recorded external identity link stays valid.
      log.warn(
          "Firebase user missing for SellerTerminal {} subject={} - re-provisioning",
          id.value(),
          identity.getExternalSubject());
      var terminal = reader.getRequired(tenantId, id);
      var result =
          identityProvisioning.provisionSellerTerminal(
              id, terminal.terminalCode(), terminal.displayName(), newPin);
      if (!identity.getExternalSubject().equals(result.externalSubject())) {
        throw new IllegalStateException(
            "Re-provisioned Firebase subject "
                + result.externalSubject()
                + " does not match recorded identity "
                + identity.getExternalSubject(),
            ex);
      }
    }
  }
}
