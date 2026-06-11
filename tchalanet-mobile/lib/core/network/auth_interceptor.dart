import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter_appauth/flutter_appauth.dart';

import '../config/app_config.dart';
import '../storage/token_storage.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(
    this._dio,
    this._tokenStorage,
    this._onSessionInvalidated, {
    FlutterAppAuth? appAuth,
  }) : _appAuth = appAuth ?? const FlutterAppAuth();

  final Dio _dio;
  final TokenStorage _tokenStorage;
  final void Function() _onSessionInvalidated;
  final FlutterAppAuth _appAuth;
  Future<String?>? _refreshing;

  AuthorizationServiceConfiguration get _serviceConfig =>
      const AuthorizationServiceConfiguration(
        authorizationEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/auth',
        tokenEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/token',
        endSessionEndpoint:
            '$kcBaseUrl/realms/$kcRealm/protocol/openid-connect/logout',
      );

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!options.path.startsWith('/tenant/')) {
      handler.next(options);
      return;
    }

    var token = await _tokenStorage.readAccessToken();
    if (token != null && _isExpiredOrExpiringSoon(token)) {
      token = await _refreshAccessToken();
    }
    if (token != null) {
      options.headers['Authorization'] = 'Bearer $token';
    }
    handler.next(options);
  }

  @override
  Future<void> onError(
    DioException err,
    ErrorInterceptorHandler handler,
  ) async {
    final request = err.requestOptions;
    if (err.response?.statusCode != 401 ||
        request.extra['authRetry'] == true ||
        !request.path.startsWith('/tenant/')) {
      handler.next(err);
      return;
    }

    final token = await _refreshAccessToken();
    if (token == null) {
      handler.next(err);
      return;
    }

    request.extra['authRetry'] = true;
    request.headers['Authorization'] = 'Bearer $token';
    try {
      handler.resolve(await _dio.fetch<dynamic>(request));
    } on DioException catch (retryError) {
      handler.next(retryError);
    }
  }

  bool _isExpiredOrExpiringSoon(String token) {
    try {
      final parts = token.split('.');
      if (parts.length != 3) return true;
      final payload =
          jsonDecode(
                utf8.decode(base64Url.decode(base64Url.normalize(parts[1]))),
              )
              as Map<String, dynamic>;
      final exp = payload['exp'] as int?;
      if (exp == null) return true;
      final expiresAt = DateTime.fromMillisecondsSinceEpoch(exp * 1000);
      return DateTime.now().isAfter(
        expiresAt.subtract(const Duration(seconds: 60)),
      );
    } catch (_) {
      return true;
    }
  }

  Future<String?> _refreshAccessToken() {
    final active = _refreshing;
    if (active != null) return active;
    final refresh = _performRefresh();
    _refreshing = refresh;
    return refresh.whenComplete(() => _refreshing = null);
  }

  Future<String?> _performRefresh() async {
    final refreshToken = await _tokenStorage.readRefreshToken();
    if (refreshToken == null || refreshToken.isEmpty) {
      await _invalidateSession();
      return null;
    }
    try {
      final result = await _appAuth.token(
        TokenRequest(
          kcClientId,
          kcRedirectUri,
          serviceConfiguration: _serviceConfig,
          refreshToken: refreshToken,
          scopes: const ['openid', 'profile', 'email'],
        ),
      );
      final accessToken = result.accessToken;
      if (accessToken == null || accessToken.isEmpty) {
        await _invalidateSession();
        return null;
      }
      await _tokenStorage.writeAccessToken(accessToken);
      final nextRefreshToken = result.refreshToken;
      if (nextRefreshToken != null && nextRefreshToken.isNotEmpty) {
        await _tokenStorage.writeRefreshToken(nextRefreshToken);
      }
      return accessToken;
    } catch (_) {
      await _invalidateSession();
      return null;
    }
  }

  Future<void> _invalidateSession() async {
    await _tokenStorage.clear();
    _onSessionInvalidated();
  }
}
