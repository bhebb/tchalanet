import 'auth_session.dart';
import 'login_credentials.dart';

abstract interface class AuthRepository {
  Future<AuthSession> login(LoginCredentials credentials);
  Future<AuthSession?> restoreSession();
  Future<void> logout();
}
