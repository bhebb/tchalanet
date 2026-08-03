package com.tchalanet.server.platform.notification.internal.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class NotificationJpaRepositoryScopeQueryTest {

  @Test
  void appUserVisibleNotificationQueriesRequireTheActiveTenantScope() {
    var queries =
        Arrays.stream(NotificationJpaRepository.class.getMethods())
            .filter(
                method ->
                    method.getName().equals("searchVisible")
                        || method.getName().equals("countVisible"))
            .map(method -> method.getAnnotation(Query.class))
            .toList();

    assertThat(queries).hasSize(2).allSatisfy(query -> assertThat(query).isNotNull());
    assertThat(queries)
        .allSatisfy(
            query ->
                assertThat(query.value()).contains(":tenantId").contains("n.tenantId = :tenantId"));
  }
}
