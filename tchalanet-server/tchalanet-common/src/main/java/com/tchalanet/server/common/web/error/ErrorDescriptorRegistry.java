package com.tchalanet.server.common.web.error;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Validates owner-declared external error descriptors.
 *
 * <p>Owners retain their descriptor constants. This collector is intentionally not a global
 * business-code enum; boot wiring and contract tests submit the descriptors they expose.
 */
@Component
public final class ErrorDescriptorRegistry {

  private final Map<String, ErrorDescriptor> descriptors;

  /**
   * Builds the application-wide descriptor catalog during Spring startup.
   *
   * <p>Each owner contributes its own descriptors through the neutral SPI. The registry is a
   * technical validator only: it does not define business codes and it never decides how an error
   * is rendered.
   */
  public ErrorDescriptorRegistry(List<ErrorDescriptorContributor> contributors) {
    var registrations =
        contributors.stream()
            .flatMap(
                contributor ->
                    contributor.descriptors().stream()
                        .map(
                            descriptor ->
                                new DescriptorRegistration(contributor.owner(), descriptor)))
            .toList();
    this.descriptors = validate(registrations);
  }

  public Map<String, ErrorDescriptor> descriptors() {
    return descriptors;
  }

  public ErrorDescriptor require(String code) {
    var descriptor = descriptors.get(code);
    if (descriptor == null) {
      throw new IllegalArgumentException("No registered error descriptor for code");
    }
    return descriptor;
  }

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
