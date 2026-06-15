package com.tchalanet.server.features.bootstrap.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.features.bootstrap.privateruntime.PrivateBootstrapRuntimeController;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class RuntimeControllerTest {

    @Test
    void privateBootstrapExposesLegacyAliasAndCanonicalRoute() throws Exception {
        var mapping =
            PrivateBootstrapRuntimeController.class
                .getDeclaredMethod(
                    "privateBootstrap",
                    com.tchalanet.server.common.context.TchRequestContext.class)
                .getAnnotation(GetMapping.class);

        assertThat(Arrays.asList(mapping.value()))
            .containsExactlyInAnyOrder("/runtime/private", "/tenant/runtime/bootstrap");
    }
}
