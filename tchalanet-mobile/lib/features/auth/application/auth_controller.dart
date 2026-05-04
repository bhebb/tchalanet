import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../data/auth_repository_impl.dart';
import '../domain/auth_repository.dart';
import '../domain/auth_session.dart';
import '../domain/login_credentials.dart';

sealed class AuthState {}

final class AuthUnknown extends AuthState {}

final class AuthUnauthenticated extends AuthState {
  AuthUnauthenticated({this.errorMessage});
  final String? errorMessage;
}

final class AuthLoading extends AuthState {}

final class AuthAuthenticated extends AuthState {
  AuthAuthenticated(this.session);
  final AuthSession session;
}

class AuthController extends Notifier<AuthState> {
  late AuthRepository _repository;

  @override
  AuthState build() {
    _repository = ref.watch(authRepositoryProvider);
    _restore();
    return AuthUnknown();
  }

  Future<void> _restore() async {
    try {
      final session = await _repository.restoreSession();
      state = session != null ? AuthAuthenticated(session) : AuthUnauthenticated();
    } catch (_) {
      state = AuthUnauthenticated();
    }
  }

  Future<void> login(LoginCredentials credentials) async {
    state = AuthLoading();
    try {
      final session = await _repository.login(credentials);
      state = AuthAuthenticated(session);
    } catch (e) {
      state = AuthUnauthenticated(errorMessage: e.toString());
    }
  }

  Future<void> logout() async {
    await _repository.logout();
    state = AuthUnauthenticated();
  }
}

final authControllerProvider = NotifierProvider<AuthController, AuthState>(
  AuthController.new,
);
