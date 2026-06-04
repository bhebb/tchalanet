// ─── Request models ───────────────────────────────────────────────────────────

class CashierTicketLineRequest {
  const CashierTicketLineRequest({
    required this.gameCode,
    required this.betType,
    required this.selection,
    required this.stake,
    this.betOption,
  });

  final String gameCode;   // e.g. "BORLETTE"
  final String betType;    // e.g. "SHORT_SINGLE_GAME"
  final String selection;  // e.g. "42"
  final double stake;
  final int? betOption;    // 1–4, optional

  Map<String, dynamic> toJson() => {
        'gameCode': gameCode,
        'betType': betType,
        'selection': selection,
        'stake': stake,
        if (betOption != null) 'betOption': betOption,
      };
}

class CashierTicketPreviewRequest {
  const CashierTicketPreviewRequest({
    required this.terminalId,
    required this.drawId,
    required this.currency,
    required this.lines,
    this.drawChannelId,
  });

  final String terminalId;
  final String drawId;
  final String? drawChannelId;
  final String currency;
  final List<CashierTicketLineRequest> lines;

  Map<String, dynamic> toJson() => {
        'terminalId': terminalId,
        'drawId': drawId,
        if (drawChannelId != null) 'drawChannelId': drawChannelId,
        'currency': currency,
        'lines': lines.map((l) => l.toJson()).toList(),
      };
}

class CashierSellTicketRequest {
  const CashierSellTicketRequest({
    required this.terminalId,
    required this.drawId,
    required this.currency,
    required this.lines,
    this.drawChannelId,
    this.promotionChoices,
  });

  final String terminalId;
  final String drawId;
  final String? drawChannelId;
  final String currency;
  final List<CashierTicketLineRequest> lines;
  final List<Map<String, dynamic>>? promotionChoices;

  Map<String, dynamic> toJson() => {
        'terminalId': terminalId,
        'drawId': drawId,
        if (drawChannelId != null) 'drawChannelId': drawChannelId,
        'currency': currency,
        'lines': lines.map((l) => l.toJson()).toList(),
        if (promotionChoices != null) 'promotionChoices': promotionChoices,
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
    required this.decision,
    required this.issues,
    this.sellerInstruction,
    this.warning,
  });

  // Backend SaleDecision: ACCEPTABLE | REQUIRES_CHANGES | REJECTED_FINAL
  final String decision;
  final List<CashierSaleIssue> issues;
  final String? sellerInstruction;
  final String? warning;

  bool get isAccepted => decision == 'ACCEPTABLE';
  bool get requiresChanges => decision == 'REQUIRES_CHANGES';
  bool get isRejected => decision == 'REJECTED_FINAL';

  factory CashierTicketPreviewResponse.fromJson(Map<String, dynamic> json) =>
      CashierTicketPreviewResponse(
        decision: json['decision'] as String? ?? 'UNKNOWN',
        issues: (json['issues'] as List<dynamic>?)
                ?.map((e) =>
                    CashierSaleIssue.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        sellerInstruction: json['sellerInstruction'] as String?,
        warning: json['warning'] as String?,
      );
}

class CashierSellTicketResponse {
  const CashierSellTicketResponse({
    required this.outcome,
    required this.ticketId,
    required this.ticketCode,
    this.publicCode,
    this.saleStatus,
    this.backup,
    this.sellerInstruction,
  });

  // Backend SellTicketOutcome: ACCEPTED | REJECTED | PENDING_APPROVAL
  final String outcome;
  final String ticketId;
  final String ticketCode;
  final String? publicCode;
  final String? saleStatus;
  final CashierTicketBackupView? backup;
  final String? sellerInstruction;

  bool get isSold => outcome == 'ACCEPTED';
  bool get isPending => outcome == 'PENDING_APPROVAL';
  bool get isRejected => outcome == 'REJECTED';

  factory CashierSellTicketResponse.fromJson(Map<String, dynamic> json) =>
      CashierSellTicketResponse(
        outcome: json['outcome'] as String? ?? 'UNKNOWN',
        ticketId: json['ticketId'] as String? ?? '',
        ticketCode: json['ticketCode'] as String? ?? '',
        publicCode: json['publicCode'] as String?,
        saleStatus: json['saleStatus'] as String?,
        backup: json['backup'] != null
            ? CashierTicketBackupView.fromJson(
                json['backup'] as Map<String, dynamic>)
            : null,
        sellerInstruction: json['sellerInstruction'] as String?,
      );
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
  final String? drawChannelName;
  final DateTime? drawScheduledAt;
  final DateTime? placedAt;

  String get displayCode => publicCode ?? ticketCode;

  /// "Haïti • New York • Midday — 3 juin 12:00"
  String get drawLabel {
    final name = drawChannelName;
    final dt = drawScheduledAt?.toLocal();
    if (name == null && dt == null) return '—';
    if (dt == null) return name ?? '—';
    final time =
        '${dt.hour.toString().padLeft(2, '0')}:${dt.minute.toString().padLeft(2, '0')}';
    final day = '${dt.day} ${_monthFr(dt.month)}';
    final suffix = '$day $time';
    return name != null ? '$name — $suffix' : suffix;
  }

  static String _monthFr(int m) => const [
        '', 'jan', 'fév', 'mars', 'avr', 'mai', 'juin',
        'juil', 'août', 'sep', 'oct', 'nov', 'déc'
      ][m];

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
    required this.potentialPayoutCents,
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
  final int potentialPayoutCents;
  final bool promotional;
  final String? promotionLabel;

  String get formattedStake =>
      (stakeAmountCents / 100).toStringAsFixed(2);
  String get formattedPayout =>
      (potentialPayoutCents / 100).toStringAsFixed(2);

  factory CashierTicketLineDetail.fromJson(Map<String, dynamic> json) =>
      CashierTicketLineDetail(
        lineNumber: (json['lineNumber'] as num?)?.toInt() ?? 0,
        gameCode: json['gameCode'] as String? ?? '',
        gameLabel: json['gameLabel'] as String? ?? '',
        betType: json['betType'] as String? ?? '',
        betTypeLabel: json['betTypeLabel'] as String?,
        selection: json['selection'] as String? ?? '',
        stakeAmountCents: (json['stakeAmountCents'] as num?)?.toInt() ?? 0,
        potentialPayoutCents:
            (json['potentialPayoutCents'] as num?)?.toInt() ?? 0,
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
    super.drawChannelName,
    super.drawScheduledAt,
    super.placedAt,
    this.cancelledAt,
    this.outletName,
    this.terminalCode,
    this.sellerDisplayName,
    this.lines = const [],
    this.stakeCents = 0,
    this.potentialPayoutCents = 0,
    this.charges = const [],
  });

  final DateTime? cancelledAt;
  final String? outletName;
  final String? terminalCode;
  final String? sellerDisplayName;
  final List<CashierTicketLineDetail> lines;
  final int stakeCents;
  final int potentialPayoutCents;
  final List<CashierTicketChargeDetail> charges;

  String get formattedStake => (stakeCents / 100).toStringAsFixed(2);
  String get formattedPotentialPayout =>
      (potentialPayoutCents / 100).toStringAsFixed(2);

  factory CashierTicketDetailsView.fromJson(Map<String, dynamic> json) =>
      CashierTicketDetailsView(
        id: json['id'] as String? ?? '',
        ticketCode: json['ticketCode'] as String? ?? '',
        publicCode: json['publicCode'] as String?,
        status: json['status'] as String? ?? 'UNKNOWN',
        totalAmountCents: (json['totalAmountCents'] as num?)?.toInt() ?? 0,
        currency: json['currency'] as String? ?? 'HTG',
        drawId: json['drawId'] as String?,
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
        lines: (json['lines'] as List<dynamic>?)
                ?.map((e) => CashierTicketLineDetail.fromJson(
                    e as Map<String, dynamic>))
                .toList() ??
            const [],
        stakeCents: (json['stakeCents'] as num?)?.toInt() ?? 0,
        potentialPayoutCents:
            (json['potentialPayoutCents'] as num?)?.toInt() ?? 0,
        charges: (json['charges'] as List<dynamic>?)
                ?.map((e) => CashierTicketChargeDetail.fromJson(
                    e as Map<String, dynamic>))
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

  bool get isPayable =>
      availableActions.any((a) => a.type == 'PAY_WINNER' && a.enabled);

  factory CashierTicketVerificationResponse.fromJson(
          Map<String, dynamic> json) =>
      CashierTicketVerificationResponse(
        status: json['status'] as String? ?? 'UNKNOWN',
        severity: json['severity'] as String? ?? 'INFO',
        ticketId: json['ticketId'] as String?,
        titleKey: json['titleKey'] as String?,
        messageKey: json['messageKey'] as String?,
        params: json['params'] as Map<String, dynamic>?,
        availableActions: (json['availableActions'] as List<dynamic>?)
                ?.map((e) => CashierAction.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
      );
}
