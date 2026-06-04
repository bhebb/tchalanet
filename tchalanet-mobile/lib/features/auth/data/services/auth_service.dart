import 'package:flutter_appauth/flutter_appauth.dart';

import '../../../../core/config/app_config.dart';
import '../../../../core/network/api_exception.dart';

class AuthService {
/// Wraps flutter_appauth for Keycloak Authorization Code + PKCE.
/// Does NOT use the authenticated API Dio client.
  const AuthService({FlutterAppAuth? appAuth})
      : _appAuth = appAuth ?? const FlutterAppAuth();

  final FlutterAppAuth _appAuth;

  /// Endpoints explicites — évite la discovery XHR sur Flutter Web où Keycloak
  /// bloque les requêtes cross-origin depuis localhost. Sur Android/iOS, AppAuth
  /// utilise HTTPURLConnection (pas de CORS) donc les deux modes fonctionnent.
  AuthorizationServiceConfiguration get _serviceConfig =>
      const AuthorizationServiceConfiguration(
        authorizationEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/auth',
        tokenEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/token',
        endSessionEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/logout',
      );

  /// Opens the system browser for Keycloak login (PKCE flow).
  /// Throws [ApiException] if the user cancels or the exchange fails.
  Future<AuthTokenData> login() async {
    try {
      final result = await _appAuth.authorizeAndExchangeCode(
        AuthorizationTokenRequest(
          kcClientId,
          kcRedirectUri,
          serviceConfiguration: _serviceConfig,
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
          serviceConfiguration: _serviceConfig,
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
