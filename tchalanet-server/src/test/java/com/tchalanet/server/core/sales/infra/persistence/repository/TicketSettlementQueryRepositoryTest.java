package com.tchalanet.server.core.sales.infra.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

class TicketSettlementQueryRepositoryTest {

  @Test
  void existsPendingUsesSaleStatusColumn() throws Exception {
    var sql = queryValue(method("existsPending"));

    assertThat(sql).contains("t.sale_status = 'SOLD'");
    assertThat(sql).doesNotContain("t.status = 'SOLD'");
  }

  @Test
  void countPendingUsesSaleStatusColumn() throws Exception {
    var sql = queryValue(method("countPending"));

    assertThat(sql).contains("t.sale_status = 'SOLD'");
    assertThat(sql).doesNotContain("t.status = 'SOLD'");
  }

  private static Method method(String name) throws NoSuchMethodException {
    return TicketSettlementQueryRepository.class.getMethod(name, UUID.class);
  }

  private static String queryValue(Method method) {
    return method.getAnnotation(Query.class).value();
  }
}
