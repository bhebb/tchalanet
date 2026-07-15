package com.tchalanet.server.platform.identity.internal.web.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResolveLoginIdentifierRequest(
    @NotBlank
        @Size(min = 3, max = 40)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "identifier must be a username")
        String identifier) {}
