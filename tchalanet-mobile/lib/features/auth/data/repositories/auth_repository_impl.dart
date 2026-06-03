import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/storage/secure_token_storage.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/user_role.dart';
import '../models/user_session.dart';
import '../services/auth_service.dart';
import 'auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(this._service, this._tokenStorage);

  final AuthService _service;
  final TokenStorage _tokenStorage;

  @override
  @override
  Future<UserSession> login() async {
    final tokens = await _service.login();
    await _storeTokens(tokens);
    return _buildSession(tokens.accessToken);
  }

  @override
  Future<UserSession?> restoreSession() async {
    final accessToken = await _tokenStorage.readAccessToken();
    if (accessToken == null || accessToken.isEmpty) return null;

    // Attempt silent refresh if the access token is expired or close to expiry
    if (_isExpiredOrExpiringSoon(accessToken)) {
      final refreshToken = await _tokenStorage.readRefreshToken();
      if (refreshToken == null || refreshToken.isEmpty) return null;
      try {
        final tokens = await _service.refresh(refreshToken);
        await _storeTokens(tokens);
        return _buildSession(tokens.accessToken);
      } catch (_) {
        await _tokenStorage.clear();
        return null;
      }
    }

    return _buildSession(accessToken);
  }

  @override
  Future<void> logout() async {
    await _tokenStorage.clear();
  }

  Future<void> _storeTokens(AuthTokenData tokens) async {
    await _tokenStorage.writeAccessToken(tokens.accessToken);
    if (tokens.refreshToken != null) {
      await _tokenStorage.writeRefreshToken(tokens.refreshToken!);
    }
  }

  bool _isExpiredOrExpiringSoon(String token) {
    final payload = _decodeJwtPayload(token);
    final exp = payload['exp'] as int?;
    if (exp == null) return false;
    final expiresAt = DateTime.fromMillisecondsSinceEpoch(exp * 1000);
    // Refresh if less than 60 seconds remaining
    return DateTime.now().isAfter(expiresAt.subtract(const Duration(seconds: 60)));
  }

  UserSession _buildSession(String accessToken) {
    final payload = _decodeJwtPayload(accessToken);

    final sub = payload['sub'] as String?;
    final username = payload['preferred_username'] as String?;
    final displayName = payload['name'] as String?;

    // tch = custom Keycloak claim block (see keycloak-token-contract spec)
    final tch = payload['tch'] as Map<String, dynamic>?;
    final tenantCode = tch?['tenant_code'] as String?;
    final tenantId = tch?['tenant_id'] as String?;

    // roles = root-level custom claim mapped by Tchalanet Keycloak provider
    final rawRoles = payload['roles'] as List<dynamic>? ?? [];
    final roles = rawRoles
        .map((r) => UserRole.fromString(r as String?))
        .where((r) => r != UserRole.unknown)
        .toList();

    final exp = payload['exp'] as int?;
    final tokenExpiresAt = exp != null
        ? DateTime.fromMillisecondsSinceEpoch(exp * 1000)
        : null;

    return UserSession(
      authenticated: true,
      userId: sub,
      username: username,
      displayName: displayName,
      tenantId: tenantId,
      tenantCode: tenantCode,
      roles: roles,
      tokenExpiresAt: tokenExpiresAt,
    );
  }

  Map<String, dynamic> _decodeJwtPayload(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return {};
      final normalized = base64Url.normalize(parts[1]);
      final decoded = utf8.decode(base64Url.decode(normalized));
      return jsonDecode(decoded) as Map<String, dynamic>;
    } catch (_) {
      return {};
    }
  }
}

final authRepositoryProvider = Provider<AuthRepository>((ref) {
  return AuthRepositoryImpl(
    const AuthService(),
    ref.watch(tokenStorageProvider),
  );
});
