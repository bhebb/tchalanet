package com.tchalanet.server.core.sales.api.model.receipt;

import java.util.List;

public final class TicketReceiptI18nKeys {

    public static final String TICKET = "receipt.ticket";
    public static final String PUBLIC_CODE = "receipt.public_code";
    public static final String SALE_TIMESTAMP = "receipt.sale_timestamp";
    public static final String TERMINAL = "receipt.terminal";
    public static final String SELLER = "receipt.seller";
    public static final String DRAW_SECTION = "receipt.section.draw";
    public static final String DRAW_FULL_PREFIX = "receipt.draw.full_prefix";
    public static final String DRAW_TIME = "receipt.draw_time";
    public static final String LINE_HEADER_NO = "receipt.line.header.no";
    public static final String LINE_HEADER_STAKE = "receipt.line.header.stake";
    public static final String LINE_HEADER_PAYOUT = "receipt.line.header.payout";
    public static final String TOTAL_STAKE = "receipt.total.stake";
    public static final String TOTAL_AMOUNT = "receipt.total.amount";
    public static final String TOTAL_MAX_PAYOUT = "receipt.total.max_payout";
    public static final String VERIFICATION = "receipt.verification";
    public static final String QR = "receipt.qr";
    public static final String CURRENCY_NOTE = "receipt.currency_note";
    public static final String PROMOTION = "receipt.promotion";
    public static final String PROMOTION_FREE_GAME_LINE = "receipt.promotion.free_game_line";
    public static final String PROMOTION_BOOST_ODDS = "receipt.promotion.boost_odds";
    public static final String MESSAGE_VALID_TICKET = "receipt.message.valid_ticket";
    public static final String MESSAGE_CODE = "receipt.message.code";
    public static final String MESSAGE_GAMES = "receipt.message.games";
    public static final String MESSAGE_GAME = "receipt.message.game";
    public static final String MESSAGE_AMOUNT = "receipt.message.amount";
    public static final String MESSAGE_BACKUP_CODE = "receipt.message.backup.code";
    public static final String MESSAGE_BACKUP_VERIFY = "receipt.message.backup.verify";
    public static final String CHARGE_SMS = "receipt.charge.sms";
    public static final String CHARGE_WHATSAPP = "receipt.charge.whatsapp";
    public static final String CHARGE_EMAIL = "receipt.charge.email";

    public static final List<String> ALL = List.of(
        TICKET,
        PUBLIC_CODE,
        SALE_TIMESTAMP,
        TERMINAL,
        SELLER,
        DRAW_SECTION,
        DRAW_FULL_PREFIX,
        DRAW_TIME,
        LINE_HEADER_NO,
        LINE_HEADER_STAKE,
        LINE_HEADER_PAYOUT,
        TOTAL_STAKE,
        TOTAL_AMOUNT,
        TOTAL_MAX_PAYOUT,
        VERIFICATION,
        QR,
        PROMOTION,
        PROMOTION_FREE_GAME_LINE,
        PROMOTION_BOOST_ODDS,
        MESSAGE_VALID_TICKET,
        MESSAGE_CODE,
        MESSAGE_GAMES,
        MESSAGE_GAME,
        MESSAGE_AMOUNT,
        MESSAGE_BACKUP_CODE,
        MESSAGE_BACKUP_VERIFY,
        CURRENCY_NOTE,
        CHARGE_SMS,
        CHARGE_WHATSAPP,
        CHARGE_EMAIL
    );

    private TicketReceiptI18nKeys() {
    }
}
