import 'package:flutter_riverpod/flutter_riverpod.dart';

class SessionInvalidationController extends Notifier<int> {
  @override
  int build() => 0;

  void invalidate() => state++;
}

final sessionInvalidationProvider =
    NotifierProvider<SessionInvalidationController, int>(
      SessionInvalidationController.new,
    );
