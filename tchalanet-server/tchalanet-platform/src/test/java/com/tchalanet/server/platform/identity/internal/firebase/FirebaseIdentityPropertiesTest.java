package com.tchalanet.server.platform.identity.internal.firebase;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FirebaseIdentityPropertiesTest {

    @Test
    void derivesOfficialIssuerAndDefaultJwksUriFromProjectId() {
        var properties = new FirebaseIdentityProperties("tchalanet-prod", null, null, null, "test.test");

        assertThat(properties.issuer())
            .isEqualTo("https://securetoken.google.com/tchalanet-prod");
        assertThat(properties.effectiveJwksUri()).isEqualTo(FirebaseIdentityProperties.DEFAULT_JWKS_URI);
        assertThat(properties.effectiveRevocationCheckMode())
            .isEqualTo(FirebaseRevocationCheckMode.SENSITIVE_ONLY);
    }

    @Test
    void failsClosedWithoutProjectId() {
        assertThatThrownBy(() -> new FirebaseIdentityProperties(" ", null, null, null, "test.test").requiredProjectId())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("project-id is required");
    }
}
