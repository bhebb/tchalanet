package com.tchalanet.server.features.pagemodel.dynamic.providers.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tchalanet.server.features.pagemodel.dynamic.PageModelDynamicProviderException;
import org.junit.jupiter.api.Test;

class PageModelJsonFragmentRegistryTest {

  private final PageModelJsonFragmentRegistry registry = new PageModelJsonFragmentRegistry();

  @Test
  void resolvesKnownKey() {
    assertThat(registry.resolve("private_sidebar_cashier"))
        .isEqualTo("pagemodel/fragments/private/cashier/sidebar.links.json");
  }

  @Test
  void rejectsUnknownKey() {
    assertThatThrownBy(() -> registry.resolve("../../application.yml"))
        .isInstanceOf(PageModelDynamicProviderException.class)
        .hasMessageContaining("Unknown JSON fragment key");
  }
}
