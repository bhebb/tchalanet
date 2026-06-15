import '../../../../core/auth/auth_token_client.dart';
import '../models/user_session.dart';

abstract interface class AuthRepository {
  Future<UserSession> login(AuthCredentials credentials);
  Future<UserSession?> restoreSession();
  Future<void> logout();
}
