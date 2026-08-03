package com.tchalanet.server.platform.identity.internal.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class AppUserJpaRepositoryNotificationAudienceQueryTest {

  @Test
  void tenantAdminDeliveryQueryIncludesOwnersAndAdministrators() throws Exception {
    Method method =
        AppUserJpaRepository.class.getMethod("findTenantAdminsForNotificationDelivery", UUID.class);

    Query query = method.getAnnotation(Query.class);

    assertThat(query).isNotNull();
    assertThat(query.value())
        .contains("ar.code in ('TENANT_OWNER', 'TENANT_ADMIN')")
        .contains("exists (");
  }
}
