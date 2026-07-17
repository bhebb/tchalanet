package com.tchalanet.server.core.drawresult.internal.application.port.out.external;

public interface ExternalResultProviderCapabilityPort {

  boolean supportsAutomaticFetch(String providerCode);
}
