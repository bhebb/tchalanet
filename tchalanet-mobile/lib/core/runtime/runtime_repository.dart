import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'runtime_models.dart';
import 'runtime_service.dart';

abstract interface class RuntimeRepository {
  Future<RuntimeBootstrap> loadPublic(String locale);
  Future<RuntimeBootstrap> loadTenant();
  Future<RuntimeStateSnapshot> refreshTenantState();
}

class RuntimeRepositoryImpl implements RuntimeRepository {
  const RuntimeRepositoryImpl(this._service);

  final RuntimeService _service;

  @override
  Future<RuntimeBootstrap> loadPublic(String locale) =>
      _service.fetchPublicBootstrap(locale);

  @override
  Future<RuntimeBootstrap> loadTenant() => _service.fetchTenantBootstrap();

  @override
  Future<RuntimeStateSnapshot> refreshTenantState() =>
      _service.fetchTenantState();
}

final runtimeRepositoryProvider = Provider<RuntimeRepository>(
  (ref) => RuntimeRepositoryImpl(ref.watch(runtimeServiceProvider)),
);
