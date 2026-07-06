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
