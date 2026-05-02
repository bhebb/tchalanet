package com.tchalanet.server.common.config.draw;

import com.tchalanet.server.core.draw.infra.config.DrawProperties;
import com.tchalanet.server.core.uslottery.infra.config.UsLotteryProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class DrawResultsCommonPropertiesBindingTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner().withUserConfiguration(PropertiesConfig.class);

  @Test
  void bindsSharedSchedulerAndDefaults() {
    contextRunner
        .withPropertyValues(
            "tch.draw.results.scheduler.active=false",
            "tch.draw.results.scheduler.tick-cron=0 */2 * * * *",
            "tch.draw.results.scheduler.apply-cron=15 */3 * * * *",
            "tch.draw.results.defaults.days-back=2",
            "tch.draw.results.defaults.max-slots=75")
        .run(
            context -> {
              var props = context.getBean(DrawResultsCommonProperties.class);

              assertThat(props.getScheduler().isActive()).isFalse();
              assertThat(props.getScheduler().getTickCron()).isEqualTo("0 */2 * * * *");
              assertThat(props.getScheduler().getApplyCron()).isEqualTo("15 */3 * * * *");
              assertThat(props.getDefaults().getDaysBack()).isEqualTo(2);
              assertThat(props.getDefaults().getMaxSlots()).isEqualTo(75);
            });
  }

  @Test
  void bindsDrawWatchdogAndSettleProperties() {
    contextRunner
        .withPropertyValues(
            "tch.draw.watchdog.provisional-stuck-minutes=45",
            "tch.draw.watchdog.provisional-cron=0 */10 * * * *",
            "tch.draw.settle.cron=0 */4 * * * *",
            "tch.draw.settle.providers=NY,TX",
            "tch.draw.settle.default-max-draws=800",
            "tch.draw.settle.max-draws-by-provider.NY=700")
        .run(
            context -> {
              var props = context.getBean(DrawProperties.class);

              assertThat(props.getWatchdog().getProvisionalStuckMinutes()).isEqualTo(45);
              assertThat(props.getWatchdog().getProvisionalCron()).isEqualTo("0 */10 * * * *");
              assertThat(props.getSettle().getCron()).isEqualTo("0 */4 * * * *");
              assertThat(props.getSettle().getProviders()).containsExactly("NY", "TX");
              assertThat(props.getSettle().getDefaultMaxDraws()).isEqualTo(800);
              assertThat(props.getSettle().getMaxDrawsByProvider()).containsEntry("NY", 700);
            });
  }

  @Test
  void stagingCanDisableUsLotteryProviders() {
    contextRunner
        .withPropertyValues("tch.us-lottery.enabled=false")
        .run(
            context -> {
              var props = context.getBean(UsLotteryProperties.class);

              assertThat(props.isEnabled()).isFalse();
            });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({
    DrawResultsCommonProperties.class,
    DrawProperties.class,
    UsLotteryProperties.class
  })
  static class PropertiesConfig {}
}
