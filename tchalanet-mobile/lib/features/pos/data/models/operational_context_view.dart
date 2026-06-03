class OperationalContextView {
  const OperationalContextView({
    this.terminalId,
    this.terminalCode,
    this.outletId,
    this.outletName,
    this.salesSessionId,
    required this.status,
    required this.source,
  });

  final String? terminalId;
  final String? terminalCode;
  final String? outletId;
  final String? outletName;
  final String? salesSessionId;
  final String status;
  final String source;

  bool get isReady =>
      terminalId != null && outletId != null && salesSessionId != null;
  bool get hasTerminal => terminalId != null;
  bool get hasOutlet => outletId != null;
  bool get hasSession => salesSessionId != null;

  static const missing = OperationalContextView(
    status: 'MISSING',
    source: 'client',
  );

  factory OperationalContextView.fromJson(Map<String, dynamic> json) =>
      OperationalContextView(
        terminalId: json['terminalId'] as String?,
        terminalCode: json['terminalCode'] as String?,
        outletId: json['outletId'] as String?,
        outletName: json['outletName'] as String?,
        salesSessionId: json['salesSessionId'] as String?,
        status: json['status'] as String? ?? 'UNKNOWN',
        source: json['source'] as String? ?? 'client',
      );
}
