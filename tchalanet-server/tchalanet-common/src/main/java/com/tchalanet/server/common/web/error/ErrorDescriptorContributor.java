package com.tchalanet.server.common.web.error;

import java.util.Collection;

/**
 * Owner-published error descriptors collected and validated when the application starts.
 *
 * <p>The common layer owns this SPI, while each domain keeps its code definitions close to its
 * public API. Contributors must not create descriptors dynamically from requests or exceptions.
 */
public interface ErrorDescriptorContributor {

  /** Stable technical/domain owner used only for registration diagnostics. */
  String owner();

  /** Immutable declarations exposed by this owner. */
  Collection<ErrorDescriptor> descriptors();
}
