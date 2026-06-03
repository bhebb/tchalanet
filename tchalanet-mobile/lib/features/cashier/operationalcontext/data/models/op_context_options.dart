class OutletOption {
  const OutletOption({
    required this.outletId,
    required this.name,
    this.kind,
  });

  final String outletId;
  final String name;
  final String? kind;

  factory OutletOption.fromJson(Map<String, dynamic> json) => OutletOption(
        outletId: json['outletId'] as String? ?? '',
        name: json['name'] as String? ?? '',
        kind: json['kind'] as String?,
      );
}

class TerminalOption {
  const TerminalOption({
    required this.terminalId,
    required this.outletId,
    required this.canSell,
    this.label,
    this.kind,
  });

  final String terminalId;
  final String outletId;
  final String? label;
  final String? kind;
  final bool canSell;

  String get displayLabel => label?.isNotEmpty == true ? label! : terminalId.substring(0, 8).toUpperCase();

  factory TerminalOption.fromJson(Map<String, dynamic> json) => TerminalOption(
        terminalId: json['terminalId'] as String? ?? '',
        outletId: json['outletId'] as String? ?? '',
        label: json['label'] as String?,
        kind: json['kind'] as String?,
        canSell: json['canSell'] as bool? ?? true,
      );
}

class OpContextDefaults {
  const OpContextDefaults({this.outletId, this.terminalId});

  final String? outletId;
  final String? terminalId;

  factory OpContextDefaults.fromJson(Map<String, dynamic> json) =>
      OpContextDefaults(
        outletId: json['outletId'] as String?,
        terminalId: json['terminalId'] as String?,
      );
}

class OpContextOptionsView {
  const OpContextOptionsView({
    required this.outlets,
    required this.terminals,
    required this.defaults,
  });

  final List<OutletOption> outlets;
  final List<TerminalOption> terminals;
  final OpContextDefaults defaults;

  /// Single outlet + single terminal → auto-select without showing picker.
  bool get canAutoSelect => outlets.length == 1 && terminals.length == 1;

  /// Terminals for a specific outlet.
  List<TerminalOption> terminalsForOutlet(String outletId) =>
      terminals.where((t) => t.outletId == outletId).toList();

  factory OpContextOptionsView.fromJson(Map<String, dynamic> json) =>
      OpContextOptionsView(
        outlets: (json['outlets'] as List<dynamic>?)
                ?.map((e) => OutletOption.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        terminals: (json['terminals'] as List<dynamic>?)
                ?.map((e) => TerminalOption.fromJson(e as Map<String, dynamic>))
                .toList() ??
            [],
        defaults: json['defaults'] != null
            ? OpContextDefaults.fromJson(json['defaults'] as Map<String, dynamic>)
            : const OpContextDefaults(),
      );
}
