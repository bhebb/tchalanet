package com.tchalanet.server.external.ports;

public interface FeatureFlagPort {
  boolean isEnabled(String flagName);
}
