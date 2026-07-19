// ─── Draw ─────────────────────────────────────────────────────────────────────

class CashierAvailableDrawView {
  const CashierAvailableDrawView({
    required this.drawId,
    required this.channelLabel,
    required this.gameCodes,
    required this.status,
    required this.providerDate,
    required this.providerTime,
    required this.providerTimezone,
    required this.localDate,
    required this.localTime,
    required this.localTimezone,
    this.drawChannelId,
    this.channelCode,
    this.scheduledAt,
    this.cutoffAt,
    this.cutoffLabel,
  });

  final String drawId;
  final String? drawChannelId;
  final String? channelCode;
  final String channelLabel;
  final List<String> gameCodes;
  final String status;
  final DateTime? scheduledAt;
  final DateTime? cutoffAt;

  // Computed on the client from cutoffAt
  final String? cutoffLabel;
  final String providerDate;
  final String providerTime;
  final String providerTimezone;
  final String localDate;
  final String localTime;
  final String localTimezone;

  bool get isOpen => status == 'OPEN' || status == 'SCHEDULED';

  String get formattedCutoff {
    if (cutoffAt == null) return '';
    final diff = cutoffAt!.difference(DateTime.now());
    if (diff.isNegative) return 'Clôturé';
    if (diff.inHours > 0) {
      return 'Clôture dans ${diff.inHours}h ${diff.inMinutes.remainder(60).toString().padLeft(2, '0')}m';
    }
    if (diff.inMinutes > 0) return 'Clôture dans ${diff.inMinutes}m';
    return 'Clôture imminente';
  }

  String get providerScheduleLabel =>
      _scheduleLabel(providerDate, providerTime, providerTimezone);

  String get localScheduleLabel =>
      _scheduleLabel(localDate, localTime, localTimezone);

  factory CashierAvailableDrawView.fromJson(Map<String, dynamic> json) =>
      CashierAvailableDrawView(
        drawId: json['drawId'] as String? ?? '',
        drawChannelId: json['drawChannelId'] as String?,
        channelCode: json['channelCode'] as String?,
        channelLabel: json['channelLabel'] as String? ?? '',
        gameCodes:
            (json['gameCodes'] as List<dynamic>?)
                ?.map((e) => e as String)
                .toList() ??
            [],
        status: json['status'] as String? ?? 'UNKNOWN',
        scheduledAt: json['scheduledAt'] != null
            ? DateTime.tryParse(json['scheduledAt'] as String)
            : null,
        cutoffAt: json['cutoffAt'] != null
            ? DateTime.tryParse(json['cutoffAt'] as String)
            : null,
        providerDate: json['providerDate'] as String,
        providerTime: json['providerTime'] as String,
        providerTimezone: json['providerTimezone'] as String,
        localDate: json['localDate'] as String,
        localTime: json['localTime'] as String,
        localTimezone: json['localTimezone'] as String,
      );
}

String _scheduleLabel(String date, String time, String timezone) {
  final normalizedTime = time.length < 5 ? time : time.substring(0, 5);
  return [
    date,
    normalizedTime,
    timezone,
  ].where((part) => part.isNotEmpty).join(' · ');
}

// ─── Game ─────────────────────────────────────────────────────────────────────

class CashierBetOptionResponse {
  const CashierBetOptionResponse({
    required this.code,
    required this.label,
    this.description,
    this.selectionHint,
    required this.selectionDigits,
    required this.selectionSegments,
  });

  final int code; // 1–4
  final String label;
  final String? description;
  final String? selectionHint;
  final int selectionDigits;
  final int selectionSegments;

  factory CashierBetOptionResponse.fromJson(Map<String, dynamic> json) =>
      CashierBetOptionResponse(
        code: (json['code'] as num?)?.toInt() ?? 0,
        label: json['label'] as String? ?? '',
        description: json['description'] as String?,
        selectionHint: json['selectionHint'] as String?,
        selectionDigits: (json['selectionDigits'] as num).toInt(),
        selectionSegments: (json['selectionSegments'] as num).toInt(),
      );
}

class CashierGameOptionResponse {
  const CashierGameOptionResponse({
    required this.gameCode,
    required this.gameLabel,
    required this.betType,
    required this.betTypeLabel,
    required this.requiresOption,
    required this.options,
    this.selectionHint,
    required this.selectionDigits,
    required this.selectionSegments,
  });

  final String gameCode; // e.g. "BORLETTE"
  final String gameLabel;
  final String betType; // e.g. "SHORT_SINGLE_GAME"
  final String betTypeLabel;
  final bool requiresOption;
  final List<CashierBetOptionResponse> options;
  final String? selectionHint; // e.g. "Entrez un numéro 1-100"
  final int selectionDigits;
  final int selectionSegments;

  CashierSelectionShape selectionShapeFor(int? optionCode) {
    final matchingOptions = options.where(
      (option) => option.code == optionCode,
    );
    final option = matchingOptions.isEmpty ? null : matchingOptions.first;
    return CashierSelectionShape(
      digits: option?.selectionDigits ?? selectionDigits,
      segments: option?.selectionSegments ?? selectionSegments,
    );
  }

  factory CashierGameOptionResponse.fromJson(Map<String, dynamic> json) =>
      CashierGameOptionResponse(
        gameCode: json['gameCode'] as String? ?? '',
        gameLabel: json['gameLabel'] as String? ?? '',
        betType: json['betType'] as String? ?? '',
        betTypeLabel: json['betTypeLabel'] as String? ?? '',
        requiresOption: json['requiresOption'] as bool? ?? false,
        options:
            (json['options'] as List<dynamic>?)
                ?.map(
                  (e) => CashierBetOptionResponse.fromJson(
                    e as Map<String, dynamic>,
                  ),
                )
                .toList() ??
            [],
        selectionHint: json['selectionHint'] as String?,
        selectionDigits: (json['selectionDigits'] as num).toInt(),
        selectionSegments: (json['selectionSegments'] as num).toInt(),
      );
}

class CashierSelectionShape {
  const CashierSelectionShape({required this.digits, required this.segments});

  final int digits;
  final int segments;
}
