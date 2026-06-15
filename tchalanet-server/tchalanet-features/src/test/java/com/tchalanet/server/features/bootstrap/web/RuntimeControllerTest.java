package com.tchalanet.server.features.bootstrap.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;

class RuntimeControllerTest {

  @Test
  void exposesProviderNeutralPrivateRuntimeWithLegacyAlias() throws Exception {
    var mapping =
        RuntimeController.class
            .getDeclaredMethod(
                "privateBootstrap",
                com.tchalanet.server.common.context.TchRequestContext.class)
            .getAnnotation(GetMapping.class);

    assertThat(Arrays.asList(mapping.value()))
        .containsExactlyInAnyOrder("/runtime/private", "/tenant/runtime/bootstrap");
  }
}

