class HaitiNumbers {
  const HaitiNumbers({this.lot1, this.lot2, this.lot3, this.lot4});

  final String? lot1;
  final String? lot2;
  final String? lot3;
  final String? lot4;

  List<String> get nonEmpty =>
      [lot1, lot2, lot3, lot4].whereType<String>().toList();

  bool get hasNumbers => nonEmpty.isNotEmpty;

  factory HaitiNumbers.fromJson(Map<String, dynamic>? json) {
    final lots = json?['lots'] as Map<String, dynamic>?;
    return HaitiNumbers(
      lot1: lots?['LOT1'] as String?,
      lot2: lots?['LOT2'] as String?,
      lot3: lots?['LOT3'] as String?,
      lot4: lots?['LOT4'] as String?,
    );
  }
}

class DrawResultView {
  const DrawResultView({this.status, this.quality, this.numbers});

  final String? status;
  final String? quality;
  final HaitiNumbers? numbers;

  factory DrawResultView.fromJson(Map<String, dynamic> json) => DrawResultView(
    status: json['status'] as String?,
    quality: json['quality'] as String?,
    numbers: HaitiNumbers.fromJson(json['haiti'] as Map<String, dynamic>?),
  );
}

class NextDrawView {
  const NextDrawView({
    required this.countdownSeconds,
    required this.status,
    this.expectedAt,
  });

  final int countdownSeconds;
  final String status;
  final DateTime? expectedAt;

  bool get isUpcoming => countdownSeconds > 0 && status != 'DONE';

  String get formattedCountdown {
    if (countdownSeconds <= 0) return '—';
    final h = countdownSeconds ~/ 3600;
    final m = (countdownSeconds % 3600) ~/ 60;
    final s = countdownSeconds % 60;
    if (h > 0) return '${h}h ${m.toString().padLeft(2, '0')}m';
    if (m > 0) return '${m}m ${s.toString().padLeft(2, '0')}s';
    return '${s}s';
  }

  factory NextDrawView.fromJson(Map<String, dynamic> json) => NextDrawView(
    countdownSeconds: (json['countdownSeconds'] as num?)?.toInt() ?? 0,
    status: json['status'] as String? ?? 'UNKNOWN',
    expectedAt: json['expectedAt'] != null
        ? DateTime.tryParse(json['expectedAt'] as String)
        : null,
  );
}

class DrawSlotView {
  const DrawSlotView({
    required this.slotKey,
    required this.label,
    this.drawTime,
    this.next,
    this.latest,
  });

  final String slotKey;
  final String label;
  final String? drawTime; // "HH:MM:SS" — display first 5 chars

  final NextDrawView? next;
  final DrawResultView? latest;

  String get displayDrawTime => drawTime != null && drawTime!.length >= 5
      ? drawTime!.substring(0, 5)
      : '';

  factory DrawSlotView.fromJson(Map<String, dynamic> json) => DrawSlotView(
    slotKey: json['slotKey'] as String? ?? '',
    label: json['label'] as String? ?? '',
    drawTime: json['drawTime'] as String?,
    next: json['next'] != null
        ? NextDrawView.fromJson(json['next'] as Map<String, dynamic>)
        : null,
    latest: json['latest'] != null
        ? DrawResultView.fromJson(json['latest'] as Map<String, dynamic>)
        : null,
  );
}

/// A selectable public result slot returned by `/public/draw-results/slots`.
class PublicDrawResultSlot {
  const PublicDrawResultSlot({
    required this.slotKey,
    required this.provider,
    required this.label,
  });

  final String slotKey;
  final String provider;
  final String label;

  factory PublicDrawResultSlot.fromJson(Map<String, dynamic> json) =>
      PublicDrawResultSlot(
        slotKey: json['slotKey'] as String? ?? '',
        provider: json['provider'] as String? ?? '',
        label: json['label'] as String? ?? '',
      );
}

/// One completed result. Server labels are retained as an accessibility fallback;
/// seller-facing screens resolve provider and slot codes through local i18n first.
class PublicDrawResultRow {
  const PublicDrawResultRow({
    required this.drawResultId,
    required this.slotKey,
    required this.provider,
    required this.drawChannelLabel,
    required this.resultDate,
    required this.drawTime,
    required this.status,
    required this.numbers,
  });

  final String drawResultId;
  final String slotKey;
  final String provider;
  final String drawChannelLabel;
  final String resultDate;
  final String drawTime;
  final String status;
  final List<String> numbers;

  String get displayDrawTime =>
      drawTime.length >= 5 ? drawTime.substring(0, 5) : drawTime;

  factory PublicDrawResultRow.fromJson(Map<String, dynamic> json) =>
      PublicDrawResultRow(
        drawResultId: json['drawResultId'] as String? ?? '',
        slotKey: json['slotKey'] as String? ?? '',
        provider: json['provider'] as String? ?? '',
        drawChannelLabel: json['drawChannelLabel'] as String? ?? '',
        resultDate: json['resultDate'] as String? ?? '',
        drawTime: json['drawTime'] as String? ?? '',
        status: json['status'] as String? ?? 'UNKNOWN',
        numbers:
            (json['numbers'] as List<dynamic>?)
                ?.whereType<String>()
                .where((number) => number.isNotEmpty)
                .toList(growable: false) ??
            const [],
      );
}

class PublicDrawResultHistory {
  const PublicDrawResultHistory({
    required this.items,
    required this.page,
    required this.totalItems,
    required this.totalPages,
  });

  final List<PublicDrawResultRow> items;
  final int page;
  final int totalItems;
  final int totalPages;

  factory PublicDrawResultHistory.fromJson(Map<String, dynamic> json) =>
      PublicDrawResultHistory(
        items:
            (json['items'] as List<dynamic>?)
                ?.map(
                  (item) => PublicDrawResultRow.fromJson(
                    item as Map<String, dynamic>,
                  ),
                )
                .toList(growable: false) ??
            const [],
        page: (json['page'] as num?)?.toInt() ?? 0,
        totalItems: (json['totalItems'] as num?)?.toInt() ?? 0,
        totalPages: (json['totalPages'] as num?)?.toInt() ?? 0,
      );
}
