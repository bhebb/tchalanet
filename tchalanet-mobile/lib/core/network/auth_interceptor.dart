import 'dart:convert';

import 'package:dio/dio.dart';

import '../auth/auth_token_client.dart';
import '../storage/token_storage.dart';

class AuthInterceptor extends Interceptor {
  AuthInterceptor(
    this._dio,
    this._tokenStorage,
    this._tokenClient,
    this._onSessionInvalidated,
  );

  final Dio _dio;
  final TokenStorage _tokenStorage;
  final AuthTokenClient _tokenClient;
  final void Function() _onSessionInvalidated;
  Future<String?>? _refreshing;

  @override
  Future<void> onRequest(
    RequestOptions options,
    RequestInterceptorHandler handler,
  ) async {
    if (!_requiresAuth(options.path)) {
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
        !_requiresAuth(request.path)) {
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

  bool _requiresAuth(String path) =>
      path.startsWith('/tenant/') || path == '/runtime/private';

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
    try {
      final result = await _tokenClient.refresh(
        await _tokenStorage.readRefreshToken(),
      );
      final accessToken = result.accessToken;
      if (accessToken.isEmpty) {
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
