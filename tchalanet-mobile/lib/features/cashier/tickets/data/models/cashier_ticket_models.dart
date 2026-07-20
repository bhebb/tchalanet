// ─── Request models ───────────────────────────────────────────────────────────

class CashierTicketLineRequest {
  const CashierTicketLineRequest({
    required this.lineNumber,
    required this.gameCode,
    required this.betType,
    required this.selection,
    required this.stakeAmount,
    this.betOption,
  });

  final int lineNumber;
  final String gameCode; // e.g. "BORLETTE"
  final String betType; // e.g. "SHORT_SINGLE_GAME"
  final String selection; // e.g. "42"
  final double stakeAmount;
  final int? betOption; // 1–4, optional

  Map<String, dynamic> toJson() => {
    'lineNumber': lineNumber,
    'gameCode': gameCode,
    'betType': betType,
    'selection': selection,
    'stakeAmount': stakeAmount,
    if (betOption != null) 'betOption': betOption,
  };
}

class CashierTicketPreviewRequest {
  const CashierTicketPreviewRequest({
    required this.drawId,
    required this.currency,
    required this.lines,
    this.drawChannelId,
  });

  final String drawId;
  final String? drawChannelId;
  final String currency;
  final List<CashierTicketLineRequest> lines;

  Map<String, dynamic> toJson() => {
    'drawId': drawId,
    if (drawChannelId != null) 'drawChannelId': drawChannelId,
    'currency': currency,
    'lines': lines.map((l) => l.toJson()).toList(),
  };
}

class CashierVerifyTicketRequest {
  const CashierVerifyTicketRequest({required this.scannedValue});

  final String scannedValue;

  Map<String, dynamic> toJson() => {'scannedValue': scannedValue};
}

// ─── Response models ──────────────────────────────────────────────────────────

class CashierTicketBackupView {
  const CashierTicketBackupView({
    this.displayCode,
    this.verificationShortUrl,
    this.shareableText,
  });

  final String? displayCode;
  final String? verificationShortUrl;
  final String? shareableText;

  factory CashierTicketBackupView.fromJson(Map<String, dynamic> json) =>
      CashierTicketBackupView(
        displayCode: json['displayCode'] as String?,
        verificationShortUrl: json['verificationShortUrl'] as String?,
        shareableText: json['shareableText'] as String?,
      );
}

class CashierSaleIssue {
  const CashierSaleIssue({
    required this.code,
    this.message,
    this.params = const {},
  });

  final String code;
  final String? message;
  final Map<String, dynamic> params;

  factory CashierSaleIssue.fromJson(Map<String, dynamic> json) =>
      CashierSaleIssue(
        code: json['code'] as String? ?? '',
        message: json['message'] as String?,
        params: (json['params'] as Map<String, dynamic>?) ?? const {},
      );
}

class CashierTicketPreviewResponse {
  const CashierTicketPreviewResponse({
    required this.status,
    this.preparationId,
    this.expiresAt,
    this.currency,
    this.totalAmount,
    this.lines = const [],
    this.promotionLines = const [],
    this.notices = const [],
  });

  // Backend SalePreparationStatus: DRAFT | CONFIRMED | EXPIRED | CANCELLED
  final String status;
  final String? preparationId;
  final DateTime? expiresAt;
  final String? currency;
  final double? totalAmount;
  final List<CashierPreparationLine> lines;
  final List<CashierPreparationPromotionLine> promotionLines;
  final List<CashierPreparationNotice> notices;

  bool get isAccepted => status == 'DRAFT' && preparationId != null;

  factory CashierTicketPreviewResponse.fromJson(Map<String, dynamic> json) =>
      CashierTicketPreviewResponse(
        status: json['status'] as String? ?? 'UNKNOWN',
        preparationId: json['preparationId']?.toString(),
        expiresAt: json['expiresAt'] != null
            ? DateTime.tryParse(json['expiresAt'] as String)
            : null,
        currency: json['currency'] as String?,
        totalAmount: (json['totalAmount'] as num?)?.toDouble(),
        lines:
            (json['lines'] as List<dynamic>?)
                ?.whereType<Map<String, dynamic>>()
                .map(CashierPreparationLine.fromJson)
                .toList() ??
            const [],
        promotionLines:
            (json['promotionLines'] as List<dynamic>?)
                ?.whereType<Map<String, dynamic>>()
                .map(CashierPreparationPromotionLine.fromJson)
                .toList() ??
            const [],
        notices:
            (json['notices'] as List<dynamic>?)
                ?.whereType<Map<String, dynamic>>()
                .map(CashierPreparationNotice.fromJson)
                .toList() ??
            const [],
      );
}

class CashierPreparationLine {
  const CashierPreparationLine({
    required this.lineNumber,
    required this.gameCode,
    required this.betType,
    required this.selection,
    required this.stakeAmount,
    required this.origin,
    this.betOption,
  });

  final int lineNumber;
  final String gameCode;
  final String betType;
  final int? betOption;
  final String selection;
  final double stakeAmount;
  final String origin;

  factory CashierPreparationLine.fromJson(Map<String, dynamic> json) =>
      CashierPreparationLine(
        lineNumber: (json['lineNumber'] as num?)?.toInt() ?? 0,
        gameCode: json['gameCode'] as String? ?? '',
        betType: json['betType'] as String? ?? '',
        betOption: (json['betOption'] as num?)?.toInt(),
        selection: json['selection'] as String? ?? '',
        stakeAmount: (json['stakeAmount'] as num?)?.toDouble() ?? 0,
        origin: json['origin'] as String? ?? '',
      );
}

class CashierPreparationPromotionLine {
  const CashierPreparationPromotionLine({
    required this.lineRef,
    required this.gameCode,
    required this.betType,
    required this.selection,
    required this.selectionSource,
    required this.choiceMode,
    required this.promotionRuleKey,
    required this.promotionEffectType,
    required this.regenerable,
    required this.regenerationsRemaining,
    this.betOption,
  });

  final String lineRef;
  final String gameCode;
  final String betType;
  final int? betOption;
  final String selection;
  final String? selectionSource;
  final String? choiceMode;
  final String? promotionRuleKey;
  final String? promotionEffectType;
  final bool regenerable;
  final int regenerationsRemaining;

  bool get isMaryaj => gameCode == 'MARYAJ';
  bool get isGenerated => selectionSource == 'PROMOTION_GENERATED';

  factory CashierPreparationPromotionLine.fromJson(Map<String, dynamic> json) =>
      CashierPreparationPromotionLine(
        lineRef: json['lineRef'] as String? ?? '',
        gameCode: json['gameCode'] as String? ?? '',
        betType: json['betType'] as String? ?? '',
        betOption: (json['betOption'] as num?)?.toInt(),
        selection: json['selection'] as String? ?? '',
        selectionSource: json['selectionSource'] as String?,
        choiceMode: json['choiceMode'] as String?,
        promotionRuleKey: json['promotionRuleKey'] as String?,
        promotionEffectType: json['promotionEffectType'] as String?,
        regenerable: json['regenerable'] as bool? ?? false,
        regenerationsRemaining:
            (json['regenerationsRemaining'] as num?)?.toInt() ?? 0,
      );
}

class CashierPreparationNotice {
  const CashierPreparationNotice({required this.code, required this.severity});

  final String code;
  final String severity;

  String get translationKey => switch (code) {
    'promotion.decision.applied' => 'common.preparation.promotion_applied',
    'promotion.terminal_override.applied' =>
      'common.preparation.terminal_override_applied',
    'sales.approval_required' => 'common.preparation.approval_required',
    _ => 'common.preparation.notice',
  };

  factory CashierPreparationNotice.fromJson(Map<String, dynamic> json) =>
      CashierPreparationNotice(
        code: json['code'] as String? ?? 'unknown',
        severity: json['severity'] as String? ?? 'INFO',
      );
}

abstract final class CashierPreparationCopy {
  static const totalKey = 'common.preparation.total';
  static const maryajFreeKey = 'common.preparation.maryaj_free';
  static const freeGameKey = 'common.preparation.free_game';
  static const freeKey = 'common.preparation.free';
  static const autoSelectionKey = 'common.preparation.auto_selection';
  static const sellerSelectionKey = 'common.preparation.seller_selection';
  static const genericNoticeKey = 'common.preparation.notice';
}

class CashierSellTicketResponse {
  const CashierSellTicketResponse({
    required this.preparationId,
    required this.alreadyConfirmed,
    required this.outcome,
    this.ticketId,
    required this.ticketCode,
    this.publicCode,
    this.saleStatus,
    this.backup,
    this.sellerInstruction,
    this.issues = const [],
    this.notices = const [],
  });

  final String? preparationId;
  final bool alreadyConfirmed;
  // Backend SellTicketOutcome: ACCEPTED | REJECTED | PENDING_APPROVAL.
  // It is null on an idempotent replay, where alreadyConfirmed is true.
  final String outcome;
  final String? ticketId;
  final String ticketCode;
  final String? publicCode;
  final String? saleStatus;
  final CashierTicketBackupView? backup;
  final String? sellerInstruction;
  final List<CashierSaleIssue> issues;
  final List<CashierPreparationNotice> notices;

  bool get isSold =>
      ticketId != null && (outcome == 'ACCEPTED' || alreadyConfirmed);
  bool get isPending => outcome == 'PENDING_APPROVAL';
  bool get isRejected => outcome == 'REJECTED';

  factory CashierSellTicketResponse.fromJson(Map<String, dynamic> json) {
    final sale = json['sale'] as Map<String, dynamic>?;
    final ticket = sale?['ticket'] as Map<String, dynamic>?;
    final backup = sale?['backup'] ?? json['backup'];
    return CashierSellTicketResponse(
      preparationId: json['preparationId']?.toString(),
      alreadyConfirmed: json['alreadyConfirmed'] as bool? ?? false,
      outcome: sale?['outcome'] as String? ?? 'CONFIRMED',
      ticketId: json['ticketId']?.toString() ?? ticket?['ticketId']?.toString(),
      ticketCode: ticket?['ticketCode'] as String? ?? '',
      publicCode: ticket?['publicCode'] as String?,
      saleStatus: ticket?['saleStatus'] as String?,
      backup: backup is Map<String, dynamic>
          ? CashierTicketBackupView.fromJson(backup)
          : null,
      sellerInstruction: sale?['sellerInstruction'] as String?,
      issues:
          (sale?['issues'] as List<dynamic>?)
              ?.whereType<Map<String, dynamic>>()
              .map(CashierSaleIssue.fromJson)
              .toList() ??
          const [],
      notices:
          (sale?['notices'] as List<dynamic>?)
              ?.whereType<Map<String, dynamic>>()
              .map(CashierPreparationNotice.fromJson)
              .toList() ??
          const [],
    );
  }
}

class CashierAction {
  const CashierAction({
    required this.type,
    required this.labelKey,
    required this.enabled,
    required this.params,
  });

  final String type;
  final String labelKey;
  final bool enabled;
  final Map<String, dynamic> params;

  factory CashierAction.fromJson(Map<String, dynamic> json) => CashierAction(
    type: json['type'] as String? ?? '',
    labelKey: json['labelKey'] as String? ?? '',
    enabled: json['enabled'] as bool? ?? false,
    params: (json['params'] as Map<String, dynamic>?) ?? {},
  );
}

class CashierTicketSummaryView {
  const CashierTicketSummaryView({
    required this.id,
    required this.ticketCode,
    required this.status,
    required this.totalAmountCents,
    required this.currency,
    this.publicCode,
    this.drawId,
    this.resultSlotKey,
    this.resultProvider,
    this.drawChannelName,
    this.drawScheduledAt,
    this.placedAt,
  });

  final String id;
  final String ticketCode;
  final String? publicCode;
  final String status; // PLACED | CANCELLED | VOIDED
  final int totalAmountCents;
  final String currency;
  final String? drawId;
  final String? resultSlotKey;
  final String? resultProvider;
  final String? drawChannelName;
  final DateTime? drawScheduledAt;
  final DateTime? placedAt;

  String get displayCode => publicCode ?? ticketCode;

  /// Fallback identity for catalog entries that do not yet expose a stable
  /// provider/slot pair. Seller views prefer localized stable codes.
  String get drawLabel {
    final name = drawChannelName;
    final dt = drawScheduledAt?.toLocal();
    if (name == null && dt == null) return '—';
    if (dt == null) return name ?? '—';
    final time =
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    final day =
        '${dt.day.toString().padLeft(2, '0')}/'
        '${dt.month.toString().padLeft(2, '0')}/${dt.year}';
    final suffix = '$day $time';
    return name != null ? '$name — $suffix' : suffix;
  }

  String get formattedAmount {
    final amount = totalAmountCents / 100;
    return '${amount.toStringAsFixed(2)} $currency';
  }

  factory CashierTicketSummaryView.fromJson(Map<String, dynamic> json) =>
      CashierTicketSummaryView(
        id: json['id'] as String? ?? '',
        ticketCode: json['ticketCode'] as String? ?? '',
        publicCode: json['publicCode'] as String?,
        status: json['status'] as String? ?? 'UNKNOWN',
        totalAmountCents: (json['totalAmountCents'] as num?)?.toInt() ?? 0,
        currency: json['currency'] as String? ?? 'HTG',
        drawId: json['drawId'] as String?,
        resultSlotKey: json['resultSlotKey'] as String?,
        resultProvider: json['resultProvider'] as String?,
        drawChannelName: json['drawChannelName'] as String?,
        drawScheduledAt: json['drawScheduledAt'] != null
            ? DateTime.tryParse(json['drawScheduledAt'] as String)
            : null,
        placedAt: json['placedAt'] != null
            ? DateTime.tryParse(json['placedAt'] as String)
            : null,
      );
}

// ─── Ticket line ──────────────────────────────────────────────────────────────

class CashierTicketLineDetail {
  const CashierTicketLineDetail({
    required this.lineNumber,
    required this.gameCode,
    required this.gameLabel,
    required this.betType,
    this.betTypeLabel,
    required this.selection,
    required this.stakeAmountCents,
    this.promotional = false,
    this.promotionLabel,
  });

  final int lineNumber;
  final String gameCode;
  final String gameLabel;
  final String betType;
  final String? betTypeLabel;
  final String selection;
  final int stakeAmountCents;
  final bool promotional;
  final String? promotionLabel;

  String get formattedStake => (stakeAmountCents / 100).toStringAsFixed(2);

  factory CashierTicketLineDetail.fromJson(Map<String, dynamic> json) =>
      CashierTicketLineDetail(
        lineNumber: (json['lineNumber'] as num?)?.toInt() ?? 0,
        gameCode: json['gameCode'] as String? ?? '',
        gameLabel: json['gameLabel'] as String? ?? '',
        betType: json['betType'] as String? ?? '',
        betTypeLabel: json['betTypeLabel'] as String?,
        selection: json['selection'] as String? ?? '',
        stakeAmountCents: (json['stakeAmountCents'] as num?)?.toInt() ?? 0,
        promotional: json['promotional'] as bool? ?? false,
        promotionLabel: json['promotionLabel'] as String?,
      );
}

// ─── Ticket charge ────────────────────────────────────────────────────────────

class CashierTicketChargeDetail {
  const CashierTicketChargeDetail({
    this.type,
    this.label,
    required this.amountCents,
    this.waived = false,
    this.waivedLabel,
  });

  final String? type;
  final String? label;
  final int amountCents;
  final bool waived;
  final String? waivedLabel;

  String get displayLabel => label ?? type ?? '';
  String get formattedAmount => (amountCents / 100).toStringAsFixed(2);

  factory CashierTicketChargeDetail.fromJson(Map<String, dynamic> json) =>
      CashierTicketChargeDetail(
        type: json['type'] as String?,
        label: json['label'] as String?,
        amountCents: (json['amountCents'] as num?)?.toInt() ?? 0,
        waived: json['waived'] as bool? ?? false,
        waivedLabel: json['waivedLabel'] as String?,
      );
}

// ─── Ticket detail ────────────────────────────────────────────────────────────

class CashierTicketDetailsView extends CashierTicketSummaryView {
  const CashierTicketDetailsView({
    required super.id,
    required super.ticketCode,
    required super.status,
    required super.totalAmountCents,
    required super.currency,
    super.publicCode,
    super.drawId,
    super.resultSlotKey,
    super.resultProvider,
    super.drawChannelName,
    super.drawScheduledAt,
    super.placedAt,
    this.cancelledAt,
    this.outletName,
    this.terminalCode,
    this.sellerDisplayName,
    this.lines = const [],
    this.stakeCents = 0,
    this.charges = const [],
  });

  final DateTime? cancelledAt;
  final String? outletName;
  final String? terminalCode;
  final String? sellerDisplayName;
  final List<CashierTicketLineDetail> lines;
  final int stakeCents;
  final List<CashierTicketChargeDetail> charges;

  String get formattedStake => (stakeCents / 100).toStringAsFixed(2);

  factory CashierTicketDetailsView.fromJson(
    Map<String, dynamic> json,
  ) => CashierTicketDetailsView(
    id: json['id'] as String? ?? '',
    ticketCode: json['ticketCode'] as String? ?? '',
    publicCode: json['publicCode'] as String?,
    status: json['status'] as String? ?? 'UNKNOWN',
    totalAmountCents: (json['totalAmountCents'] as num?)?.toInt() ?? 0,
    currency: json['currency'] as String? ?? 'HTG',
    drawId: json['drawId'] as String?,
    resultSlotKey: json['resultSlotKey'] as String?,
    resultProvider: json['resultProvider'] as String?,
    drawChannelName: json['drawChannelName'] as String?,
    drawScheduledAt: json['drawScheduledAt'] != null
        ? DateTime.tryParse(json['drawScheduledAt'] as String)
        : null,
    placedAt: json['placedAt'] != null
        ? DateTime.tryParse(json['placedAt'] as String)
        : null,
    cancelledAt: json['cancelledAt'] != null
        ? DateTime.tryParse(json['cancelledAt'] as String)
        : null,
    outletName: json['outletName'] as String?,
    terminalCode: json['terminalCode'] as String?,
    sellerDisplayName: json['sellerDisplayName'] as String?,
    lines:
        (json['lines'] as List<dynamic>?)
            ?.map(
              (e) =>
                  CashierTicketLineDetail.fromJson(e as Map<String, dynamic>),
            )
            .toList() ??
        const [],
    stakeCents: (json['stakeCents'] as num?)?.toInt() ?? 0,
    charges:
        (json['charges'] as List<dynamic>?)
            ?.map(
              (e) =>
                  CashierTicketChargeDetail.fromJson(e as Map<String, dynamic>),
            )
            .toList() ??
        const [],
  );
}

class CashierTicketVerificationResponse {
  const CashierTicketVerificationResponse({
    required this.status,
    required this.severity,
    required this.availableActions,
    this.ticketId,
    this.titleKey,
    this.messageKey,
    this.params,
  });

  // VALID | INVALID | EXPIRED | ALREADY_PAID | PENDING
  final String status;
  final String severity;
  final String? ticketId;
  final String? titleKey;
  final String? messageKey;
  final Map<String, dynamic>? params;
  final List<CashierAction> availableActions;

  factory CashierTicketVerificationResponse.fromJson(
    Map<String, dynamic> json,
  ) => CashierTicketVerificationResponse(
    status: json['status'] as String? ?? 'UNKNOWN',
    severity: json['severity'] as String? ?? 'INFO',
    ticketId: json['ticketId'] as String?,
    titleKey: json['titleKey'] as String?,
    messageKey: json['messageKey'] as String?,
    params: json['params'] as Map<String, dynamic>?,
    availableActions:
        (json['availableActions'] as List<dynamic>?)
            ?.map((e) => CashierAction.fromJson(e as Map<String, dynamic>))
            .toList() ??
        [],
  );
}
