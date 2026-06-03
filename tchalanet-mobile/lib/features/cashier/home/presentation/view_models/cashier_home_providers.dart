import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/models/cashier_home_models.dart';
import '../../data/services/cashier_home_service.dart';

/// Full cashier home payload. Refreshable — call ref.invalidate(cashierHomeProvider)
/// after operational context selection or session open/close.
final cashierHomeProvider = FutureProvider<CashierHomeResponse>((ref) async {
  return ref.watch(cashierHomeServiceProvider).fetchHome();
});

/// Readiness badges and blockers — polled separately so the home screen
/// can show attention indicators without reloading the full home payload.
final cashierReadinessProvider =
    FutureProvider<CashierReadinessResponse>((ref) async {
  return ref.watch(cashierHomeServiceProvider).fetchReadiness();
});
