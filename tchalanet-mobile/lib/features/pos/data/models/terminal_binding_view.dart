class TerminalBindingView {
  const TerminalBindingView({
    required this.terminalId,
    required this.terminalCode,
    required this.bound,
    this.outletId,
    this.outletName,
    this.boundAt,
  });

  final String terminalId;
  final String terminalCode;
  final bool bound;
  final String? outletId;
  final String? outletName;
  final DateTime? boundAt;

  static const unbound = TerminalBindingView(
    terminalId: '',
    terminalCode: '',
    bound: false,
  );

  factory TerminalBindingView.fromJson(Map<String, dynamic> json) =>
      TerminalBindingView(
        terminalId: json['terminalId'] as String? ?? '',
        terminalCode: json['terminalCode'] as String? ?? '',
        bound: json['bound'] as bool? ?? false,
        outletId: json['outletId'] as String?,
        outletName: json['outletName'] as String?,
        boundAt: json['boundAt'] != null
            ? DateTime.tryParse(json['boundAt'] as String)
            : null,
      );
}
