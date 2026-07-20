package com.tchalanet.server.features.bootstrap.privateruntime.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tchalanet.server.common.json.utils.JsonUtils;
import com.tchalanet.server.features.bootstrap.privateruntime.model.PrivateBootstrapSpace;
import com.tchalanet.server.features.pagemodel.dynamic.providers.json.PageModelJsonFragmentRegistry;
import org.junit.jupiter.api.Test;

class PrivateShellNavigationResolverTest {

  @Test
  void cashierDoesNotRequireAWebNavigationDrawerFragment() {
    var resolver =
        new PrivateShellNavigationResolver(
            mock(PageModelJsonFragmentRegistry.class), mock(JsonUtils.class));

    assertThat(resolver.resolve(PrivateBootstrapSpace.CASHIER)).isEmpty();
  }
}
