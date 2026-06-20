package com.tchalanet.server.platform.identity.internal.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "tch.identity.temporary-credentials")
public record TemporaryCredentialProperties(Admin admin, Seller seller) {

  public Admin admin() {
    return admin == null ? new Admin(null, null, null) : admin;
  }

  public Seller seller() {
    return seller == null ? new Seller(null, null) : seller;
  }

  public record Admin(Boolean enabled, String defaultPassword, Boolean generateInProd) {
    public boolean effectiveEnabled() {
      return enabled == null || enabled;
    }

    public String effectiveDefaultPassword() {
      return defaultPassword == null || defaultPassword.isBlank() ? "Changeme1!" : defaultPassword;
    }

    public boolean effectiveGenerateInProd() {
      return generateInProd == null || generateInProd;
    }
  }

  public record Seller(Integer defaultPinLength, Boolean generatePin) {
    public int effectiveDefaultPinLength() {
      return defaultPinLength == null || defaultPinLength < 4 ? 6 : defaultPinLength;
    }

    public boolean effectiveGeneratePin() {
      return generatePin == null || generatePin;
    }
  }
}
