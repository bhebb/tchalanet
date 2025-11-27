package com.tchalanet.server.uslottery.infra.adapter;

import com.tchalanet.server.uslottery.domain.dto.LatestDrawDto;
import com.tchalanet.server.uslottery.domain.model.UsLotteryProvider;
import com.tchalanet.server.uslottery.domain.ports.out.LatestDrawProviderClient;
import com.tchalanet.server.uslottery.infra.config.ResultProviderProperties;
import java.util.List;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class ResultProviderRouter
    implements LatestDrawProviderClient { // Implements LatestDrawProviderClient

  private final ResultProviderProperties props;
  private final LatestDrawProviderClient lotteryApi; // may be null if not configured
  private final LatestDrawProviderClient fakeAdapter; // available in dev/profile

  public ResultProviderRouter(
      ResultProviderProperties props,
      java.util.Optional<LatestDrawProviderClient> lotteryApiOpt, // Changed type
      java.util.Optional<LatestDrawProviderClient> fakeOpt) { // Changed type
    this.props = props;
    this.lotteryApi = lotteryApiOpt.orElse(null);
    this.fakeAdapter = fakeOpt.orElse(null);
  }

  @Override
  public UsLotteryProvider provider() {
    if (lotteryApi != null) return lotteryApi.provider();
    if (fakeAdapter != null) return fakeAdapter.provider();
    return UsLotteryProvider.FLORIDA; // fallback
  }

  @Override
  public List<LatestDrawDto> fetchLatestDraws() {
    String mode = props.getMode();
    if (mode == null || mode.isBlank() || mode.equalsIgnoreCase("disabled")) return List.of();

    switch (mode.toLowerCase()) {
      case "fake":
        return fakeAdapter != null ? fakeAdapter.fetchLatestDraws() : List.of();
      case "api":
        if (lotteryApi == null) return List.of();
        return lotteryApi.fetchLatestDraws();
      case "api_then_fake":
        List<LatestDrawDto> maybe = List.of();
        if (lotteryApi != null) maybe = lotteryApi.fetchLatestDraws();
        if (!maybe.isEmpty()) return maybe;
        return fakeAdapter != null ? fakeAdapter.fetchLatestDraws() : List.of();
      default:
        return List.of();
    }
  }
}
