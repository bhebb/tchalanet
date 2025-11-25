package com.tchalanet.server.draw.infra.external;

import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev", "local"})
public class FakeExternalDrawResultAdapter implements ExternalDrawResultPort {

  private final Random random = new Random();

  @Override
  public Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query) {
    List<String> numbers =
        IntStream.range(0, 3).mapToObj(i -> String.format("%02d", random.nextInt(100))).toList();

    return Optional.of(
        new ExternalDrawResult(
            query.channelCode(), query.drawDate(), numbers, Map.of("source", "FAKE")));
  }
}
