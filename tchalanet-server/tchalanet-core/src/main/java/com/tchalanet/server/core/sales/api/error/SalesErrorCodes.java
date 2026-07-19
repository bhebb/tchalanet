package com.tchalanet.server.core.sales.api.error;

import com.tchalanet.server.common.web.error.ErrorAudience;
import com.tchalanet.server.common.web.error.ErrorCategory;
import com.tchalanet.server.common.web.error.ErrorDescriptor;
import com.tchalanet.server.common.web.error.ErrorRetryPolicy;
import java.util.Set;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

/** Stable client-facing sale gates owned by core sales. */
@UtilityClass
public class SalesErrorCodes {

  private static final Set<ErrorAudience> SALE_AUDIENCES =
      Set.of(ErrorAudience.WEB_ADMIN, ErrorAudience.MOBILE);

  public static final ErrorDescriptor DRAW_NOT_OPEN = conflict("sales.draw.not_open");
  public static final ErrorDescriptor DRAW_CHANNEL_INACTIVE =
      conflict("sales.draw_channel.inactive");
  public static final ErrorDescriptor RESULT_SLOT_INACTIVE = conflict("sales.result_slot.inactive");
  public static final ErrorDescriptor DRAW_CUTOFF_ELAPSED = conflict("sales.draw.cutoff_elapsed");
  public static final ErrorDescriptor DRAW_CHANNEL_MISMATCH =
      conflict("sales.draw_channel.mismatch");
  public static final ErrorDescriptor SELLER_TERMINAL_REQUIRED =
      businessRule("sales.seller_terminal.required");
  public static final ErrorDescriptor SELLER_TERMINAL_CANNOT_SELL =
      businessRule("sales.seller_terminal.cannot_sell");
  public static final ErrorDescriptor TENANT_DISABLED = businessRule("sales.tenant.disabled");
  public static final ErrorDescriptor TENANT_BUSINESS_CLOSED =
      businessRule("sales.tenant.business_closed");
  public static final ErrorDescriptor DRAW_REQUIRED = validation("sales.draw_required");
  public static final ErrorDescriptor DRAW_CHANNEL_REQUIRED =
      validation("sales.draw_channel_required");
  public static final ErrorDescriptor CURRENCY_REQUIRED = validation("sales.currency_required");
  public static final ErrorDescriptor LINES_REQUIRED = validation("sales.lines_required");
  public static final ErrorDescriptor DUPLICATE_LINE_NUMBER =
      validation("sales.duplicate_line_number");
  public static final ErrorDescriptor TENANT_GAME_NOT_CONFIGURED =
      validation("sales.tenant_game_not_configured");
  public static final ErrorDescriptor TENANT_GAME_DISABLED =
      validation("sales.tenant_game_disabled");
  public static final ErrorDescriptor TENANT_GAME_NOT_VISIBLE_IN_POS =
      validation("sales.tenant_game_not_visible_in_pos");
  public static final ErrorDescriptor INVALID_LINE_NUMBER = validation("sales.invalid_line_number");
  public static final ErrorDescriptor GAME_REQUIRED = validation("sales.game_required");
  public static final ErrorDescriptor BET_TYPE_REQUIRED = validation("sales.bet_type_required");
  public static final ErrorDescriptor UNSUPPORTED_BET_TYPE =
      validation("sales.unsupported_bet_type");
  public static final ErrorDescriptor SELECTION_REQUIRED = validation("sales.selection_required");
  public static final ErrorDescriptor INVALID_STAKE_AMOUNT =
      validation("sales.invalid_stake_amount");
  public static final ErrorDescriptor SELECTION_INVALID = validation("sales.selection_invalid");
  public static final ErrorDescriptor TENANT_BET_OPTION_NOT_CONFIGURED =
      validation("sales.tenant_bet_option_not_configured");
  public static final ErrorDescriptor BET_OPTION_NOT_ALLOWED =
      validation("sales.bet_option_not_allowed");
  public static final ErrorDescriptor BET_OPTION_OUT_OF_RANGE =
      validation("sales.bet_option_out_of_range");
  public static final ErrorDescriptor STAKE_BELOW_TENANT_MIN =
      validation("sales.stake_below_tenant_min");
  public static final ErrorDescriptor STAKE_ABOVE_TENANT_MAX =
      validation("sales.stake_above_tenant_max");
  public static final ErrorDescriptor TENANT_BET_TYPE_NOT_CONFIGURED =
      validation("sales.tenant_bet_type_not_configured");
  public static final ErrorDescriptor BET_OPTION_REQUIRED = validation("sales.bet_option_required");
  public static final ErrorDescriptor TENANT_BET_OPTION_DISABLED =
      validation("sales.tenant_bet_option_disabled");
  public static final ErrorDescriptor TENANT_BET_OPTION_NOT_VISIBLE_IN_POS =
      validation("sales.tenant_bet_option_not_visible_in_pos");
  public static final ErrorDescriptor TICKET_NOT_FOUND = notFound("ticket.not_found");
  public static final ErrorDescriptor TICKET_PRINT_VIEW_NOT_FOUND =
      notFound("ticket.print_view.not_found");
  public static final ErrorDescriptor TICKET_FILTER_INVALID_DATE_RANGE =
      validation("ticket.filter.invalid_date_range");
  public static final ErrorDescriptor TICKET_FILTER_INVALID_SORT =
      validation("ticket.filter.invalid_sort");
  public static final ErrorDescriptor TICKET_FILTER_INVALID_STATUS =
      validation("ticket.filter.invalid_status");
  public static final ErrorDescriptor PREPARATION_NOT_FOUND =
      notFound("sales.preparation.not_found");
  public static final ErrorDescriptor PREPARATION_PROMOTION_LINE_NOT_FOUND =
      notFound("sales.preparation.promotion_line_not_found");
  public static final ErrorDescriptor PREPARATION_ALREADY_CONFIRMED =
      conflict("sales.preparation.already_confirmed");
  public static final ErrorDescriptor PREPARATION_EXPIRED = conflict("sales.preparation.expired");
  public static final ErrorDescriptor PREPARATION_NOT_CONFIRMABLE =
      conflict("sales.preparation.not_confirmable");
  public static final ErrorDescriptor PREPARATION_LINE_NOT_REGENERABLE =
      conflict("sales.preparation.line_not_regenerable");
  public static final ErrorDescriptor PREPARATION_MAX_REGENERATIONS_REACHED =
      conflict("sales.preparation.max_regenerations_reached");
  public static final ErrorDescriptor PREPARATION_CANCELLED =
      conflict("sales.preparation.cancelled");
  public static final ErrorDescriptor TICKET_PRINT_VOIDED_NOT_ALLOWED =
      businessRule("ticket.print.voided_not_allowed");
  public static final ErrorDescriptor TICKET_PRINT_SALE_NOT_ACCEPTED =
      businessRule("ticket.print.sale_not_accepted");
  public static final ErrorDescriptor TICKET_REPRINT_REASON_REQUIRED =
      validation("ticket.reprint.reason_required");
  public static final ErrorDescriptor TICKET_REPRINT_REASON_TOO_LONG =
      validation("ticket.reprint.reason_too_long");

  public static Set<ErrorDescriptor> all() {
    return Set.of(
        DRAW_NOT_OPEN,
        DRAW_CHANNEL_INACTIVE,
        RESULT_SLOT_INACTIVE,
        DRAW_CUTOFF_ELAPSED,
        DRAW_CHANNEL_MISMATCH,
        SELLER_TERMINAL_REQUIRED,
        SELLER_TERMINAL_CANNOT_SELL,
        TENANT_DISABLED,
        TENANT_BUSINESS_CLOSED,
        DRAW_REQUIRED,
        DRAW_CHANNEL_REQUIRED,
        CURRENCY_REQUIRED,
        LINES_REQUIRED,
        DUPLICATE_LINE_NUMBER,
        TENANT_GAME_NOT_CONFIGURED,
        TENANT_GAME_DISABLED,
        TENANT_GAME_NOT_VISIBLE_IN_POS,
        INVALID_LINE_NUMBER,
        GAME_REQUIRED,
        BET_TYPE_REQUIRED,
        UNSUPPORTED_BET_TYPE,
        SELECTION_REQUIRED,
        INVALID_STAKE_AMOUNT,
        SELECTION_INVALID,
        TENANT_BET_OPTION_NOT_CONFIGURED,
        BET_OPTION_NOT_ALLOWED,
        BET_OPTION_OUT_OF_RANGE,
        STAKE_BELOW_TENANT_MIN,
        STAKE_ABOVE_TENANT_MAX,
        TENANT_BET_TYPE_NOT_CONFIGURED,
        BET_OPTION_REQUIRED,
        TENANT_BET_OPTION_DISABLED,
        TENANT_BET_OPTION_NOT_VISIBLE_IN_POS,
        TICKET_NOT_FOUND,
        TICKET_PRINT_VIEW_NOT_FOUND,
        TICKET_FILTER_INVALID_DATE_RANGE,
        TICKET_FILTER_INVALID_SORT,
        TICKET_FILTER_INVALID_STATUS,
        PREPARATION_NOT_FOUND,
        PREPARATION_PROMOTION_LINE_NOT_FOUND,
        PREPARATION_ALREADY_CONFIRMED,
        PREPARATION_EXPIRED,
        PREPARATION_NOT_CONFIRMABLE,
        PREPARATION_LINE_NOT_REGENERABLE,
        PREPARATION_MAX_REGENERATIONS_REACHED,
        PREPARATION_CANCELLED,
        TICKET_PRINT_VOIDED_NOT_ALLOWED,
        TICKET_PRINT_SALE_NOT_ACCEPTED,
        TICKET_REPRINT_REASON_REQUIRED,
        TICKET_REPRINT_REASON_TOO_LONG);
  }

  private static ErrorDescriptor conflict(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.CONFLICT,
        HttpStatus.CONFLICT,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        SALE_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor businessRule(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.BUSINESS_RULE,
        HttpStatus.FORBIDDEN,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        SALE_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor validation(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.VALIDATION,
        HttpStatus.BAD_REQUEST,
        ErrorRetryPolicy.AFTER_USER_ACTION,
        SALE_AUDIENCES,
        Set.of());
  }

  private static ErrorDescriptor notFound(String code) {
    return new ErrorDescriptor(
        code,
        ErrorCategory.NOT_FOUND,
        HttpStatus.NOT_FOUND,
        ErrorRetryPolicy.NEVER,
        SALE_AUDIENCES,
        Set.of());
  }
}
