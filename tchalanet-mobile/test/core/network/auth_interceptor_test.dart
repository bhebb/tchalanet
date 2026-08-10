import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/auth/auth_token_client.dart';
import 'package:tchalanet_mobile/core/network/auth_interceptor.dart';
import 'package:tchalanet_mobile/core/storage/token_storage.dart';

void main() {
  test(
    'tenant request refreshes Firebase token when storage is empty',
    () async {
      final storage = _FakeTokenStorage();
      final tokenClient = _FakeTokenClient(_jwt(expired: false));
      final adapter = _RespondingAdapter((request) {
        return ResponseBody.fromString(
          '{"data":{}}',
          200,
          headers: {
            Headers.contentTypeHeader: [Headers.jsonContentType],
          },
        );
      });
      var invalidated = false;
      final dio = Dio(BaseOptions(baseUrl: 'https://api.test'));
      dio.httpClientAdapter = adapter;
      dio.interceptors.add(
        AuthInterceptor(dio, storage, tokenClient, () => invalidated = true),
      );

      await dio.post<dynamic>('/tenant/sales/preparations');

      expect(tokenClient.refreshCount, 1);
      expect(storage.accessToken, tokenClient.accessToken);
      expect(adapter.authorizationHeader, 'Bearer ${tokenClient.accessToken}');
      expect(invalidated, isFalse);
    },
  );

  test(
    'business 403 after token refresh does not invalidate session',
    () async {
      final storage = _FakeTokenStorage(accessToken: _jwt(expired: true));
      final tokenClient = _FakeTokenClient(_jwt(expired: false));
      var invalidations = 0;
      final dio = Dio(BaseOptions(baseUrl: 'https://api.test'));
      dio.httpClientAdapter = _RespondingAdapter((request) {
        return ResponseBody.fromString(
          jsonEncode({
            'status': 403,
            'code': 'sales.seller_terminal.cannot_sell',
            'category': 'business_rule',
          }),
          403,
          headers: {
            Headers.contentTypeHeader: [Headers.jsonContentType],
          },
        );
      });
      dio.interceptors.add(
        AuthInterceptor(dio, storage, tokenClient, () => invalidations++),
      );

      await expectLater(
        dio.post<dynamic>('/tenant/sales/preparations/prep-1/confirm'),
        throwsA(
          isA<DioException>().having(
            (e) => e.response?.statusCode,
            'statusCode',
            403,
          ),
        ),
      );

      expect(tokenClient.refreshCount, 1);
      expect(storage.cleared, isFalse);
      expect(invalidations, 0);
    },
  );
}

class _FakeTokenStorage implements TokenStorage {
  _FakeTokenStorage({this.accessToken});

  String? accessToken;
  String? refreshToken;
  bool cleared = false;

  @override
  Future<void> clear() async {
    cleared = true;
    accessToken = null;
    refreshToken = null;
  }

  @override
  Future<String?> readAccessToken() async => accessToken;

  @override
  Future<String?> readRefreshToken() async => refreshToken;

  @override
  Future<void> writeAccessToken(String token) async => accessToken = token;

  @override
  Future<void> writeRefreshToken(String token) async => refreshToken = token;
}

class _FakeTokenClient implements AuthTokenClient {
  _FakeTokenClient(this.accessToken);

  final String accessToken;
  var refreshCount = 0;

  @override
  Future<AuthTokenData> login(AuthCredentials credentials) async =>
      AuthTokenData(accessToken: accessToken);

  @override
  Future<void> logout() async {}

  @override
  Future<AuthTokenData> refresh([String? refreshToken]) async {
    refreshCount++;
    return AuthTokenData(accessToken: accessToken);
  }
}

class _RespondingAdapter implements HttpClientAdapter {
  _RespondingAdapter(this._handler);

  final ResponseBody Function(RequestOptions request) _handler;
  String? authorizationHeader;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    authorizationHeader = options.headers['Authorization'] as String?;
    return _handler(options);
  }

  @override
  void close({bool force = false}) {}
}

String _jwt({required bool expired}) {
  final expiresAt =
      DateTime.now()
          .add(expired ? const Duration(minutes: -5) : const Duration(hours: 1))
          .millisecondsSinceEpoch ~/
      1000;
  return [
    _base64Json({'alg': 'none', 'typ': 'JWT'}),
    _base64Json({'exp': expiresAt}),
    'signature',
  ].join('.');
}

String _base64Json(Map<String, Object?> value) =>
    base64Url.encode(utf8.encode(jsonEncode(value))).replaceAll('=', '');
