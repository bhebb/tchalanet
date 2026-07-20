import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/network/api_exception.dart';
import '../../data/repositories/change_pin_repository.dart';
import '../../data/repositories/change_pin_repository_impl.dart';

class ChangePinState {
  const ChangePinState({
    this.submitting = false,
    this.completed = false,
    this.errorKeys = const [],
  });

  final bool submitting;
  final bool completed;
  final List<String> errorKeys;
}

class ChangePinController extends Notifier<ChangePinState> {
  late ChangePinRepository _repository;

  @override
  ChangePinState build() {
    _repository = ref.watch(changePinRepositoryProvider);
    return const ChangePinState();
  }

  Future<void> submit(String newPin) async {
    if (state.submitting) return;
    state = const ChangePinState(submitting: true);
    try {
      await _repository.changePin(newPin);
      state = const ChangePinState(completed: true);
    } catch (error) {
      state = ChangePinState(errorKeys: userErrorTranslationKeys(error));
    }
  }
}

final changePinControllerProvider =
    NotifierProvider.autoDispose<ChangePinController, ChangePinState>(
      ChangePinController.new,
    );
