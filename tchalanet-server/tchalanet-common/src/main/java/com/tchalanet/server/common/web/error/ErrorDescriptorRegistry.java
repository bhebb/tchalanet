package com.tchalanet.server.common.web.error;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Validates owner-declared external error descriptors.
 *
 * <p>Owners retain their descriptor constants. This collector is intentionally not a global
 * business-code enum; boot wiring and contract tests submit the descriptors they expose.
 */
public final class ErrorDescriptorRegistry {

  private ErrorDescriptorRegistry() {}

  public static Map<String, ErrorDescriptor> validate(
      Collection<DescriptorRegistration> registrations) {
    var descriptors = new LinkedHashMap<String, ErrorDescriptor>();
    var declarationOwners = new LinkedHashMap<String, String>();

    for (var registration : registrations) {
      Objects.requireNonNull(registration, "registration");
      var previousOwner =
          declarationOwners.putIfAbsent(registration.descriptor().code(), registration.owner());
      if (previousOwner != null) {
        throw new IllegalArgumentException(
            "Duplicate error descriptor code declared by "
                + previousOwner
                + " and "
                + registration.owner());
      }
      descriptors.put(registration.descriptor().code(), registration.descriptor());
    }

    return Map.copyOf(descriptors);
  }

  public record DescriptorRegistration(String owner, ErrorDescriptor descriptor) {

    public DescriptorRegistration {
      if (owner == null || owner.isBlank()) {
        throw new IllegalArgumentException("Descriptor owner is required");
      }
      Objects.requireNonNull(descriptor, "descriptor");
    }
  }
}
