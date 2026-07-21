package com.tchalanet.server.app.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tchalanet.server.common.bus.CommandBus;
import com.tchalanet.server.common.bus.QueryBus;
import com.tchalanet.server.common.context.ContextKeys;
import com.tchalanet.server.common.context.TchActorType;
import com.tchalanet.server.common.context.TchRequestContext;
import com.tchalanet.server.common.context.scope.ApiScope;
import com.tchalanet.server.common.context.web.CurrentContextArgumentResolver;
import com.tchalanet.server.common.types.id.TenantId;
import com.tchalanet.server.common.web.error.GlobalErrorHandler;
import com.tchalanet.server.common.web.error.ProblemRest;
import com.tchalanet.server.core.pricing.api.error.PricingErrorCodes;
import com.tchalanet.server.core.pricing.internal.infra.web.admin.PricingOverrideAdminController;
import com.tchalanet.server.core.sellerterminal.api.error.SellerTerminalErrorCodes;
import com.tchalanet.server.core.sellerterminal.internal.infra.web.admin.SellerTerminalAdminController;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminSellerTerminalPricingProblemContractTest {

  private static final UUID TENANT_UUID = UUID.fromString("10000000-0000-0000-0000-000000000001");
  private static final UUID SELLER_TERMINAL_UUID =
      UUID.fromString("20000000-0000-0000-0000-000000000001");

  @Test
  void pricingValidationIsSerializedAsAStableProblemWithoutDomainProse() throws Exception {
    var commandBus = mock(CommandBus.class);
    when(commandBus.execute(any())).thenThrow(ProblemRest.of(PricingErrorCodes.ODDS_INVALID));

    var result =
        pricingMvc(commandBus)
            .perform(
                put("/admin/controls/pricing-rules")
                    .with(context())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "gameCode":"HT_BORLETTE",
                          "pricingVariantCode":"MATCH_1_2D",
                          "betType":"STRAIGHT",
                          "odds":100.0000,
                          "payoutRuleType":"STAKE_MULTIPLIER"
                        }
                        """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("pricing.odds_invalid"))
            .andExpect(jsonPath("$.category").value("validation"))
            .andExpect(jsonPath("$.retryPolicy").value("AFTER_USER_ACTION"))
            .andExpect(jsonPath("$.retryable").value(true))
            .andExpect(jsonPath("$.detail").value("Request could not be completed"))
            .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("Odds must")
        .doesNotContain("IllegalArgumentException");
  }

  @Test
  void sellerTerminalTransitionIsSerializedAsAStableProblemWithoutTerminalDiagnostics()
      throws Exception {
    var commandBus = mock(CommandBus.class);
    when(commandBus.execute(any()))
        .thenThrow(ProblemRest.of(SellerTerminalErrorCodes.STATUS_TRANSITION_INVALID));

    var result =
        sellerTerminalMvc(commandBus)
            .perform(
                patch("/admin/seller-terminals/{id}/block", SELLER_TERMINAL_UUID)
                    .with(context())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"reason\":\"Fraud review\"}"))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("sellerterminal.status_transition_invalid"))
            .andExpect(jsonPath("$.category").value("conflict"))
            .andExpect(jsonPath("$.retryPolicy").value("AFTER_USER_ACTION"))
            .andExpect(jsonPath("$.retryable").value(true))
            .andExpect(jsonPath("$.detail").value("Request could not be completed"))
            .andReturn();

    assertThat(result.getResponse().getContentAsString())
        .doesNotContain("Fraud review")
        .doesNotContain("SellerTerminalStatusException");
  }

  private static MockMvc pricingMvc(CommandBus commandBus) {
    return MockMvcBuilders.standaloneSetup(
            new PricingOverrideAdminController(commandBus, mock(QueryBus.class)))
        .setControllerAdvice(new GlobalErrorHandler())
        .setCustomArgumentResolvers(new CurrentContextArgumentResolver())
        .build();
  }

  private static MockMvc sellerTerminalMvc(CommandBus commandBus) {
    // Standalone MockMvc has no Spring context: register the typed-id converter
    // the real app provides as a bean, or path-variable binding fails with 500.
    var conversionService = new org.springframework.format.support.FormattingConversionService();
    conversionService.addConverter(
        new com.tchalanet.server.common.web.converter.StringToSellerTerminalIdConverter());
    return MockMvcBuilders.standaloneSetup(
            new SellerTerminalAdminController(commandBus, mock(QueryBus.class)))
        .setControllerAdvice(new GlobalErrorHandler())
        .setCustomArgumentResolvers(new CurrentContextArgumentResolver())
        .setConversionService(conversionService)
        .build();
  }

  private static RequestPostProcessor context() {
    var tenantId = TenantId.of(TENANT_UUID);
    var requestContext =
        new TchRequestContext(
            "tenant-contract",
            TENANT_UUID,
            "tenant-contract",
            TENANT_UUID,
            UUID.fromString("30000000-0000-0000-0000-000000000001"),
            Set.of(),
            Set.of(),
            Locale.CANADA_FRENCH,
            "req-contract",
            "127.0.0.1",
            null,
            false,
            null,
            "active",
            ApiScope.TENANT,
            null,
            tenantId,
            java.time.ZoneId.of("America/Port-au-Prince"),
            Currency.getInstance("HTG"),
            null,
            TchActorType.APP_USER,
            null,
            Set.of(),
            Set.of(),
            null);
    return request -> {
      request.setAttribute(ContextKeys.REQUEST_CONTEXT, requestContext);
      return request;
    };
  }
}
