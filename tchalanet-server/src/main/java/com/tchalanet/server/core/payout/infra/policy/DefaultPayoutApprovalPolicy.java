package com.tchalanet.server.core.payout.infra.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tchalanet.server.core.payout.application.port.out.PayoutApprovalPolicyPort;
import com.tchalanet.server.core.tenant.application.port.out.TenantSettingsReaderPort;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DefaultPayoutApprovalPolicy implements PayoutApprovalPolicyPort {

  private final BigDecimal defaultThreshold;
  private final TenantSettingsReaderPort settingsReader;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DefaultPayoutApprovalPolicy(@Value("${payout.autoapprove.threshold:10000}") BigDecimal defaultThreshold,
      TenantSettingsReaderPort settingsReader) {
    this.defaultThreshold = defaultThreshold;
    this.settingsReader = settingsReader;
  }

  @Override
  public boolean autoApprove(UUID tenantId, BigDecimal amount) {
    // 0) try consolidated JSON config `payout.config` (e.g. {"autoApprove":true, "threshold":5000})
    Optional<TenantSetting> consolidated = settingsReader.findByTenantIdAndKey(tenantId, "payout.config");
    if (consolidated.isPresent()) {
      String json = consolidated.get().getConfigValue();
      try {
        JsonNode node = objectMapper.readTree(json);
        if (node.has("autoApprove")) {
          if (node.get("autoApprove").isBoolean()) return node.get("autoApprove").asBoolean();
        }
        if (node.has("threshold")) {
          try {
            BigDecimal t = new BigDecimal(node.get("threshold").asText());
            return amount.compareTo(t) <= 0;
          } catch (Exception ex) {
            // fall through
          }
        }
      } catch (Exception ex) {
        // invalid json -> fallback to individual keys
      }
    }

    // 1) try boolean flag
    Optional<TenantSetting> boolSetting = settingsReader.findByTenantIdAndKey(tenantId, "payout.autoapprove");
    if (boolSetting.isPresent()) {
      String val = boolSetting.get().getConfigValue();
      if ("true".equalsIgnoreCase(val) || "1".equals(val)) return true;
      if ("false".equalsIgnoreCase(val) || "0".equals(val)) return false;
    }

    // 2) try threshold per-tenant
    Optional<TenantSetting> thrSetting = settingsReader.findByTenantIdAndKey(tenantId, "payout.autoapprove.threshold");
    if (thrSetting.isPresent()) {
      try {
        BigDecimal tenantThreshold = new BigDecimal(thrSetting.get().getConfigValue());
        return amount.compareTo(tenantThreshold) <= 0;
      } catch (NumberFormatException ex) {
        // fall through to default
      }
    }

    // 3) fallback default threshold
    return amount.compareTo(defaultThreshold) <= 0;
  }
}
