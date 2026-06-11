import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/features/auth/data/models/user_session.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository.dart';
import 'package:tchalanet_mobile/features/auth/data/repositories/auth_repository_impl.dart';
import 'package:tchalanet_mobile/features/auth/presentation/view_models/auth_controller.dart';

class _FailingAuthRepository implements AuthRepository {
  @override
  Future<UserSession> login() => throw Exception('sensitive provider error');

  @override
  Future<void> logout() async {}

  @override
  Future<UserSession?> restoreSession() async => null;
}

class _RacingAuthRepository implements AuthRepository {
  final restoreCompleter = Completer<UserSession?>();

  @override
  Future<UserSession> login() async => UserSession.unauthenticated;

  @override
  Future<void> logout() async {}

  @override
  Future<UserSession?> restoreSession() => restoreCompleter.future;
}

void main() {
  test(
    'login failure exposes an i18n key instead of a raw exception',
    () async {
      final container = ProviderContainer(
        overrides: [
          authRepositoryProvider.overrideWithValue(_FailingAuthRepository()),
        ],
      );
      addTearDown(container.dispose);

      container.read(authControllerProvider);
      await Future<void>.delayed(Duration.zero);
      await container.read(authControllerProvider.notifier).login();

      final state = container.read(authControllerProvider);
      expect(state, isA<AuthUnauthenticated>());
      expect((state as AuthUnauthenticated).errorKey, 'auth.login.error');
    },
  );

  test('late session restoration cannot overwrite a completed login', () async {
    final repository = _RacingAuthRepository();
    final container = ProviderContainer(
      overrides: [authRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);

    container.read(authControllerProvider);
    await container.read(authControllerProvider.notifier).login();
    final stateAfterLogin = container.read(authControllerProvider);

    repository.restoreCompleter.complete(null);
    await Future<void>.delayed(Duration.zero);

    expect(container.read(authControllerProvider), same(stateAfterLogin));
  });
}
