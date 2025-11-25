package com.tchalanet.server.draw.infra.external;

import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort;
import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort.DrawExternalQuery;
import com.tchalanet.server.draw.application.port.out.ExternalDrawResultPort.ExternalDrawResult;
import java.util.Optional;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ResultProviderRouter implements ExternalDrawResultPort {

  private final ResultProviderProperties props;
  private final ExternalDrawResultPort lotteryApi; // may be null if not configured
  private final ExternalDrawResultPort fakeAdapter; // available in dev/profile

  public ResultProviderRouter(
      ResultProviderProperties props,
      java.util.Optional<ExternalDrawResultPort> lotteryApiOpt,
      java.util.Optional<ExternalDrawResultPort> fakeOpt) {
    this.props = props;
    this.lotteryApi = lotteryApiOpt.orElse(null);
    this.fakeAdapter = fakeOpt.orElse(null);
  }

  @Override
  public Optional<ExternalDrawResult> fetchResult(DrawExternalQuery query) {
    String mode = props.getMode();
    if (mode == null || mode.isBlank() || mode.equalsIgnoreCase("disabled"))
      return Optional.empty();

    switch (mode.toLowerCase()) {
      case "fake":
        return fakeAdapter != null ? fakeAdapter.fetchResult(query) : Optional.empty();
      case "api":
        if (lotteryApi == null) return Optional.empty();
        return lotteryApi.fetchResult(query);
      case "api_then_fake":
        Optional<ExternalDrawResult> maybe = Optional.empty();
        if (lotteryApi != null) maybe = lotteryApi.fetchResult(query);
        if (maybe.isPresent()) return maybe;
        return fakeAdapter != null ? fakeAdapter.fetchResult(query) : Optional.empty();
      default:
        return Optional.empty();
    }
  }
}
