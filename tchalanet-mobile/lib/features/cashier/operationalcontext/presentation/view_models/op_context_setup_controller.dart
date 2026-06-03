import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../home/presentation/view_models/cashier_home_providers.dart';
import '../../data/models/op_context_options.dart';
import '../../data/services/cashier_op_context_service.dart';

/// Drives the setup screen: loads options, handles picker state, saves selection.
class OpContextSetupController extends Notifier<OpContextSetupState> {
  @override
  OpContextSetupState build() => const OpContextSetupState.loading();

  Future<void> loadOptions() async {
    state = const OpContextSetupState.loading();
    try {
      final options =
          await ref.read(cashierOpContextServiceProvider).fetchOptions();
      state = OpContextSetupState.loaded(options, selectedOutletId: null);
    } catch (e) {
      state = OpContextSetupState.error(e.toString());
    }
  }

  void selectOutlet(String outletId) {
    final current = state;
    if (current is! OpContextLoadedState) return;
    state = OpContextSetupState.loaded(
      current.options,
      selectedOutletId: outletId,
    );
  }

  /// Saves the outlet+terminal selection, refreshes home, signals done.
  Future<void> confirmSelection({
    required String outletId,
    required String terminalId,
  }) async {
    final current = state;
    if (current is! OpContextLoadedState) return;
    state = OpContextSetupState.selecting(current.options, current.selectedOutletId);
    try {
      await ref.read(cashierOpContextServiceProvider).saveSelection(
            outletId: outletId,
            terminalId: terminalId,
          );
      // Invalidate home so it re-fetches with the new headers
      ref.invalidate(cashierHomeProvider);
      state = const OpContextSetupState.done();
    } catch (e) {
      state = OpContextSetupState.loaded(current.options,
          selectedOutletId: current.selectedOutletId, error: e.toString());
    }
  }
}

final opContextSetupControllerProvider =
    NotifierProvider<OpContextSetupController, OpContextSetupState>(
  OpContextSetupController.new,
);

// ─── State ────────────────────────────────────────────────────────────────────

sealed class OpContextSetupState {
  const OpContextSetupState();

  const factory OpContextSetupState.loading() = OpContextLoadingState;
  const factory OpContextSetupState.loaded(
    OpContextOptionsView options, {
    required String? selectedOutletId,
    String? error,
  }) = OpContextLoadedState;
  const factory OpContextSetupState.selecting(
    OpContextOptionsView options,
    String? selectedOutletId,
  ) = OpContextSelectingState;
  const factory OpContextSetupState.error(String message) = OpContextErrorState;
  const factory OpContextSetupState.done() = OpContextDoneState;
}

final class OpContextLoadingState extends OpContextSetupState {
  const OpContextLoadingState();
}

final class OpContextLoadedState extends OpContextSetupState {
  const OpContextLoadedState(this.options,
      {required this.selectedOutletId, this.error});

  final OpContextOptionsView options;
  final String? selectedOutletId;
  final String? error;
}

final class OpContextSelectingState extends OpContextSetupState {
  const OpContextSelectingState(this.options, this.selectedOutletId);

  final OpContextOptionsView options;
  final String? selectedOutletId;
}

final class OpContextErrorState extends OpContextSetupState {
  const OpContextErrorState(this.message);

  final String message;
}

final class OpContextDoneState extends OpContextSetupState {
  const OpContextDoneState();
}
