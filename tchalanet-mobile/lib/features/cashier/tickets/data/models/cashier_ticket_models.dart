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

  // ACCEPTED | REJECTED | PENDING_REVIEW
  final String decision;
  final List<CashierSaleIssue> issues;
  final String? sellerInstruction;
  final String? warning;

  bool get isAccepted => decision == 'ACCEPTED';
  bool get isRejected => decision == 'REJECTED';

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

  // SOLD | PENDING | REJECTED
  final String outcome;
  final String ticketId;
  final String ticketCode;
  final String? publicCode;
  final String? saleStatus;
  final CashierTicketBackupView? backup;
  final String? sellerInstruction;

  bool get isSold => outcome == 'SOLD';

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

class CashierTicketVerificationResponse {
  const CashierTicketVerificationResponse({
    required this.status,
    required this.severity,
    required this.availableActions,
    this.titleKey,
    this.messageKey,
    this.params,
  });

  // VALID | INVALID | EXPIRED | ALREADY_PAID | PENDING
  final String status;
  final String severity;
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
        titleKey: json['titleKey'] as String?,
        messageKey: json['messageKey'] as String?,
        params: json['params'] as Map<String, dynamic>?,
        availableActions: (json['availableActions'] as List<dynamic>?)
                ?.map((e) => CashierAction.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
      );
}
