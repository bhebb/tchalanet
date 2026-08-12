class PosDrawTopSelection {
  const PosDrawTopSelection({
    required this.selectionKey,
    required this.stakeTotalCents,
    required this.salesCount,
  });

  final String selectionKey;
  final int stakeTotalCents;
  final int salesCount;

  factory PosDrawTopSelection.fromJson(Map<String, dynamic> json) =>
      PosDrawTopSelection(
        selectionKey: json['selectionKey'] as String? ?? '',
        stakeTotalCents: json['stakeTotalCents'] as int? ?? 0,
        salesCount: json['salesCount'] as int? ?? 0,
      );
}

class PosDrawExposureAlert {
  const PosDrawExposureAlert({
    required this.selectionKey,
    required this.stakeTotalCents,
    required this.limitCents,
    required this.ratio,
  });

  final String selectionKey;
  final int stakeTotalCents;
  final int limitCents;
  final double ratio;

  factory PosDrawExposureAlert.fromJson(Map<String, dynamic> json) =>
      PosDrawExposureAlert(
        selectionKey: json['selectionKey'] as String? ?? '',
        stakeTotalCents: json['stakeTotalCents'] as int? ?? 0,
        limitCents: json['limitCents'] as int? ?? 0,
        ratio: (json['ratio'] as num?)?.toDouble() ?? 0.0,
      );
}

class PosDrawDetailResponse {
  const PosDrawDetailResponse({
    required this.topSelections,
    this.exposureAlerts,
  });

  final List<PosDrawTopSelection> topSelections;
  final List<PosDrawExposureAlert>? exposureAlerts;

  factory PosDrawDetailResponse.fromJson(Map<String, dynamic> json) {
    final rawTop = json['topSelections'] as List<dynamic>?;
    final rawAlerts = json['exposureAlerts'] as List<dynamic>?;
    return PosDrawDetailResponse(
      topSelections:
          rawTop
              ?.map(
                (e) => PosDrawTopSelection.fromJson(e as Map<String, dynamic>),
              )
              .toList() ??
          [],
      exposureAlerts: rawAlerts
          ?.map((e) => PosDrawExposureAlert.fromJson(e as Map<String, dynamic>))
          .toList(),
    );
  }
}
