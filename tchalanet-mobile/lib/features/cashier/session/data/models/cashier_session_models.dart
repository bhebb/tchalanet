class CashierSessionView {
  const CashierSessionView({
    required this.sessionId,
    required this.status,
    this.outletId,
    this.terminalId,
    this.openedAt,
    this.closedAt,
    this.openingFloat,
    this.closingAmount,
  });

  final String sessionId;
  final String status; // OPEN | CLOSED
  final String? outletId;
  final String? terminalId;
  final DateTime? openedAt;
  final DateTime? closedAt;
  final double? openingFloat;
  final double? closingAmount;

  bool get isOpen => status == 'OPEN';

  factory CashierSessionView.fromJson(Map<String, dynamic> json) =>
      CashierSessionView(
        sessionId: json['sessionId'] as String? ?? '',
        status: json['status'] as String? ?? 'UNKNOWN',
        outletId: json['outletId'] as String?,
        terminalId: json['terminalId'] as String?,
        openedAt: json['openedAt'] != null
            ? DateTime.tryParse(json['openedAt'] as String)
            : null,
        closedAt: json['closedAt'] != null
            ? DateTime.tryParse(json['closedAt'] as String)
            : null,
        openingFloat: (json['openingFloat'] as num?)?.toDouble(),
        closingAmount: (json['closingAmount'] as num?)?.toDouble(),
      );
}

class OpenSessionRequest {
  const OpenSessionRequest({
    required this.outletId,
    required this.terminalId,
    this.openingFloat = 0.0,
  });

  final String outletId;
  final String terminalId;
  final double openingFloat;

  Map<String, dynamic> toJson() => {
        'outletId': outletId,
        'terminalId': terminalId,
        'openingFloat': openingFloat,
      };
}

class CloseSessionRequest {
  const CloseSessionRequest({
    required this.sessionId,
    required this.closingAmount,
    this.reason = 'Fermeture normale',
  });

  final String sessionId;
  final double closingAmount;
  final String reason;

  Map<String, dynamic> toJson() => {
        'sessionId': sessionId,
        'closingAmount': closingAmount,
        'reason': reason,
      };
}
