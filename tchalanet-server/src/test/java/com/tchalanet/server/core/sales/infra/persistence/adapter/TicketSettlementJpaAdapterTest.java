package com.tchalanet.server.core.sales.infra.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.tchalanet.server.common.types.enums.TicketResultStatus;
import com.tchalanet.server.common.types.enums.TicketSaleStatus;
import com.tchalanet.server.common.types.id.DrawId;
import com.tchalanet.server.core.sales.domain.model.Ticket;
import com.tchalanet.server.core.sales.infra.persistence.TicketEntity;
import com.tchalanet.server.core.sales.infra.persistence.mapper.TicketMapper;
import com.tchalanet.server.core.sales.infra.persistence.repository.TicketSettlementJpaRepository;
import java.lang.reflect.Proxy;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class TicketSettlementJpaAdapterTest {

  @Test
  void findNextBatchForDrawPassesLimitAsRepositoryPageSize() {
    var drawId = DrawId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    var cursorTime = Instant.parse("2026-05-06T12:00:00Z");
    var cursorId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    var entity = new TicketEntity();
    var repo = new RecordingTicketSettlementJpaRepository(List.of(entity));

    var adapter = new TicketSettlementJpaAdapter(repo.instance(), mapper());

    var result = adapter.findNextBatchForDraw(drawId, cursorTime, cursorId, 25);

    assertThat(repo.invocation.drawId()).isEqualTo(drawId.value());
    assertThat(repo.invocation.saleStatus()).isEqualTo(TicketSaleStatus.SOLD);
    assertThat(repo.invocation.resultStatus()).isEqualTo(TicketResultStatus.NOT_RESULTED);
    assertThat(repo.invocation.afterCreatedAt()).isEqualTo(cursorTime);
    assertThat(repo.invocation.afterId()).isEqualTo(cursorId);
    assertThat(repo.invocation.pageable().getPageNumber()).isZero();
    assertThat(repo.invocation.pageable().getPageSize()).isEqualTo(25);
    assertThat(result).hasSize(1);
  }

  @Test
  void findNextBatchForDrawUsesMinimumPageSizeOfOne() {
    var drawId = DrawId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));
    var repo = new RecordingTicketSettlementJpaRepository(List.of());

    var adapter = new TicketSettlementJpaAdapter(repo.instance(), mapper());

    adapter.findNextBatchForDraw(drawId, null, null, 0);

    assertThat(repo.invocation.afterCreatedAt()).isEqualTo(Instant.EPOCH);
    assertThat(repo.invocation.afterId()).isEqualTo(new UUID(0L, 0L));
    assertThat(repo.invocation.pageable().getPageSize()).isEqualTo(1);
  }

  private static TicketMapper mapper() {
    return new TicketMapper() {
      @Override
      public Ticket toDomain(TicketEntity entity) {
        return null;
      }
    };
  }

  private static final class RecordingTicketSettlementJpaRepository {
    private final List<TicketEntity> result;
    private Invocation invocation;

    private RecordingTicketSettlementJpaRepository(List<TicketEntity> result) {
      this.result = result;
    }

    private TicketSettlementJpaRepository instance() {
      return (TicketSettlementJpaRepository)
          Proxy.newProxyInstance(
              TicketSettlementJpaRepository.class.getClassLoader(),
              new Class<?>[] {TicketSettlementJpaRepository.class},
              (proxy, method, args) -> {
                if ("findBatchForDrawWithLines".equals(method.getName())) {
                  invocation =
                      new Invocation(
                          (UUID) args[0],
                          (TicketSaleStatus) args[1],
                          (TicketResultStatus) args[2],
                          (Instant) args[3],
                          (UUID) args[4],
                          (Pageable) args[5]);
                  return result;
                }
                throw new UnsupportedOperationException(
                    method.getName() + Arrays.toString(method.getParameterTypes()));
              });
    }
  }

  private record Invocation(
      UUID drawId,
      TicketSaleStatus saleStatus,
      TicketResultStatus resultStatus,
      Instant afterCreatedAt,
      UUID afterId,
      Pageable pageable) {}
}
