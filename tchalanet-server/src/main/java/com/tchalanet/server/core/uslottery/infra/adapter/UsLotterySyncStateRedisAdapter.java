package com.tchalanet.server.core.uslottery.infra.adapter;

import com.tchalanet.server.core.uslottery.domain.model.LatestDraw;
import com.tchalanet.server.core.uslottery.domain.ports.out.UsLotterySyncStatePort;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-based implementation of UsLotterySyncStatePort. Stores a small key per (provider,channel,date)
 * to avoid refetching the same external draw repeatedly. Keys expire after configured TTL.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnBean(StringRedisTemplate.class)
public class UsLotterySyncStateRedisAdapter implements UsLotterySyncStatePort {

  private final StringRedisTemplate redisTemplate;
  private final UsLotteryProperties props;

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

  private String keyFor(LatestDraw d) {
    var date = d.drawTimeUtc().toLocalDate();
    return String.format("uslottery:sync:%s:%s:%s", d.provider().name(), d.channelCode(), DATE_FMT.format(date));
  }

  @Override
  public boolean shouldFetch(LatestDraw probe) {
    var key = keyFor(probe);
    var exists = redisTemplate.hasKey(key);
    return exists == null || !exists;
  }

  @Override
  public void markFetchAttempt(LatestDraw probe) {
    var key = keyFor(probe);
    int ttl = props.getSyncTtlSeconds();
    redisTemplate.opsForValue().set(key, "1", ttl, TimeUnit.SECONDS);
  }
}
