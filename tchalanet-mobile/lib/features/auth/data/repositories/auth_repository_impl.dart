import 'dart:convert';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../../core/auth/auth_token_client.dart';
import '../../../../core/auth/firebase_auth_token_client.dart';
import '../../../../core/runtime/runtime_repository.dart';
import '../../../../core/storage/op_context_storage.dart';
import '../../../../core/storage/secure_token_storage.dart';
import '../../../../core/storage/token_storage.dart';
import '../models/user_role.dart';
import '../models/user_session.dart';
import 'auth_repository.dart';

class AuthRepositoryImpl implements AuthRepository {
  AuthRepositoryImpl(
    this._service,
    this._tokenStorage,
    this._opContextStorage,
    this._runtimeRepository,
  );

  final AuthTokenClient _service;
  final TokenStorage _tokenStorage;
  final OpContextStorage _opContextStorage;
  final RuntimeRepository _runtimeRepository;

  @override
  Future<UserSession> login(AuthCredentials credentials) async {
    final tokens = await _service.login(credentials);
    await _storeTokens(tokens);
    try {
      return await _buildSession(tokens.accessToken);
    } catch (_) {
      await _service.logout();
      await _tokenStorage.clear();
      rethrow;
    }
  }

  @override
  Future<UserSession?> restoreSession() async {
    final accessToken = await _tokenStorage.readAccessToken();
    if (accessToken == null || accessToken.isEmpty) return null;

    // Attempt silent refresh if the access token is expired or close to expiry
    if (_isExpiredOrExpiringSoon(accessToken)) {
      try {
        final tokens = await _service.refresh(
          await _tokenStorage.readRefreshToken(),
        );
        await _storeTokens(tokens);
        return await _buildSession(tokens.accessToken);
      } catch (_) {
        await _service.logout();
        await _tokenStorage.clear();
        return null;
      }
    }

    try {
      return await _buildSession(accessToken);
    } catch (_) {
      await _service.logout();
      await _tokenStorage.clear();
      return null;
    }
  }

  @override
  Future<void> logout() async {
    await _service.logout();
    await _tokenStorage.clear();
    await _opContextStorage.clear();
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
    return DateTime.now().isAfter(
      expiresAt.subtract(const Duration(seconds: 60)),
    );
  }

  Future<UserSession> _buildSession(String accessToken) async {
    final payload = _decodeJwtPayload(accessToken);
    final runtime = await _runtimeRepository.loadTenant();
    final user = runtime.user;
    if (user == null || user.userId == null) {
      throw StateError(
        'Authenticated runtime did not resolve an application user',
      );
    }
    final roles = runtime.roles
        .map(UserRole.fromString)
        .where((r) => r != UserRole.unknown)
        .toList();

    final exp = payload['exp'] as int?;
    final tokenExpiresAt = exp != null
        ? DateTime.fromMillisecondsSinceEpoch(exp * 1000)
        : null;

    return UserSession(
      authenticated: true,
      userId: user.userId,
      username: user.username ?? user.email,
      displayName: user.displayName ?? user.username ?? user.email,
      tenantId: runtime.tenantContext?.tenantId,
      tenantCode: runtime.tenantContext?.tenantCode,
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
    FirebaseAuthTokenClient(),
    ref.watch(tokenStorageProvider),
    ref.watch(opContextStorageProvider),
    ref.watch(runtimeRepositoryProvider),
  );
});
