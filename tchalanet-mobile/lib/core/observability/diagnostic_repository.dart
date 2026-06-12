import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'diagnostic_info.dart';

class DiagnosticRepository {
  static const _maxSize = 5;

  final List<DiagnosticInfo> _entries = [];

  void record(DiagnosticInfo info) {
    _entries.add(info);
    if (_entries.length > _maxSize) {
      _entries.removeAt(0);
    }
  }

  DiagnosticInfo? get last => _entries.isEmpty ? null : _entries.last;

  List<DiagnosticInfo> get all => List.unmodifiable(_entries);

  void clear() => _entries.clear();
}

final diagnosticRepositoryProvider = Provider<DiagnosticRepository>(
  (_) => DiagnosticRepository(),
);
