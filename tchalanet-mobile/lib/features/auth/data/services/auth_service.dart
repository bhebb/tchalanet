import 'package:flutter_appauth/flutter_appauth.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/network/api_exception.dart';

/// Wraps flutter_appauth for Keycloak Authorization Code + PKCE.
/// Does NOT use the authenticated API Dio client.
class AuthService {
  const AuthService({FlutterAppAuth? appAuth})
      : _appAuth = appAuth ?? const FlutterAppAuth();

  final FlutterAppAuth _appAuth;

  String get _discoveryUrl =>
      '$kcBaseUrl/realms/$kcRealm/.well-known/openid-configuration';

  /// Opens the system browser for Keycloak login (PKCE flow).
  /// Throws [ApiException] if the user cancels or the exchange fails.
  Future<AuthTokenData> login() async {
    try {
      final result = await _appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          kcClientId,
          kcRedirectUri,
          discoveryUrl: _discoveryUrl,
          scopes: ['openid', 'profile', 'email'],
          promptValues: ['login'],
        ),
      );
      return AuthTokenData(
        accessToken: result.accessToken ??
            (throw ApiException(message: 'Login cancelled or token missing')),
        refreshToken: result.refreshToken,
        expiresAt: result.accessTokenExpirationDateTime,
      );
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException(message: 'Auth error: $e');
    }
  }

  /// Silent token refresh using the stored refresh token.
  Future<AuthTokenData> refresh(String refreshToken) async {
    try {
      final result = await _appAuth.token(
        TokenRequest(
          kcClientId,
          kcRedirectUri,
          discoveryUrl: _discoveryUrl,
          refreshToken: refreshToken,
          scopes: ['openid', 'profile', 'email'],
        ),
      );
      return AuthTokenData(
        accessToken: result.accessToken ??
            (throw ApiException(message: 'Token refresh failed')),
        refreshToken: result.refreshToken,
        expiresAt: result.accessTokenExpirationDateTime,
      );
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException(message: 'Refresh error: $e');
    }
  }
}

class AuthTokenData {
  const AuthTokenData({
    required this.accessToken,
    this.refreshToken,
    this.expiresAt,
  });

  final String accessToken;
  final String? refreshToken;
  final DateTime? expiresAt;
}
