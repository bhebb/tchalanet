import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'runtime_models.dart';
import 'runtime_repository.dart';

const runtimePollingInterval = Duration(minutes: 10);
const runtimeForegroundRefreshAge = Duration(minutes: 2);
const runtimeRebootstrapCooldown = Duration(minutes: 1);

enum RuntimeRefreshOutcome { updated, rebootstrap, sessionExpired, failed }

class RuntimeAppState {
  const RuntimeAppState({
    this.bootstrap,
    this.snapshot,
    this.versions,
    this.lastStateRefreshAt,
    this.loading = false,
  });

  final RuntimeBootstrap? bootstrap;
  final RuntimeStateSnapshot? snapshot;
  final RuntimeVersions? versions;
  final DateTime? lastStateRefreshAt;
  final bool loading;

  RuntimeAppState copyWith({
    RuntimeBootstrap? bootstrap,
    RuntimeStateSnapshot? snapshot,
    RuntimeVersions? versions,
    DateTime? lastStateRefreshAt,
    bool? loading,
    bool clearTenant = false,
  }) => RuntimeAppState(
    bootstrap: clearTenant ? null : bootstrap ?? this.bootstrap,
    snapshot: clearTenant ? null : snapshot ?? this.snapshot,
    versions: clearTenant ? null : versions ?? this.versions,
    lastStateRefreshAt: clearTenant
        ? null
        : lastStateRefreshAt ?? this.lastStateRefreshAt,
    loading: loading ?? this.loading,
  );
}

class RuntimeController extends Notifier<RuntimeAppState> {
  late RuntimeRepository _repository;
  DateTime? _lastRebootstrapAt;
  int _revision = 0;

  @override
  RuntimeAppState build() {
    _repository = ref.watch(runtimeRepositoryProvider);
    return const RuntimeAppState();
  }

  Future<void> loadPublic(String locale) async {
    final revision = ++_revision;
    state = state.copyWith(loading: true, clearTenant: true);
    try {
      final bootstrap = await _repository.loadPublic(locale);
      if (revision != _revision) return;
      state = RuntimeAppState(bootstrap: bootstrap);
    } catch (_) {
      if (revision != _revision) return;
      state = const RuntimeAppState();
    }
  }

  Future<void> loadTenant() async {
    final revision = ++_revision;
    state = state.copyWith(loading: true, clearTenant: true);
    try {
      final bootstrap = await _repository.loadTenant();
      if (revision != _revision) return;
      state = state.copyWith(bootstrap: bootstrap, loading: false);
    } catch (_) {
      if (revision != _revision) return;
      state = state.copyWith(loading: false);
    }
  }

  Future<RuntimeRefreshOutcome> refreshTenantState() async {
    final revision = _revision;
    try {
      final snapshot = await _repository.refreshTenantState();
      if (revision != _revision) return RuntimeRefreshOutcome.failed;
      if (snapshot.status == RuntimeStatus.sessionExpired) {
        state = state.copyWith(
          snapshot: snapshot,
          versions: snapshot.versions,
          lastStateRefreshAt: DateTime.now(),
        );
        return RuntimeRefreshOutcome.sessionExpired;
      }

      final previousVersions = state.versions;
      final versionsChanged =
          previousVersions != null && previousVersions != snapshot.versions;
      final forceReload = snapshot.status == RuntimeStatus.forceReload;
      state = state.copyWith(
        snapshot: snapshot,
        versions: snapshot.versions,
        lastStateRefreshAt: DateTime.now(),
      );

      if ((versionsChanged || forceReload) && _canRebootstrap()) {
        _lastRebootstrapAt = DateTime.now();
        await loadTenant();
        return RuntimeRefreshOutcome.rebootstrap;
      }
      return RuntimeRefreshOutcome.updated;
    } catch (_) {
      return RuntimeRefreshOutcome.failed;
    }
  }

  bool shouldRefreshOnForeground(DateTime now) {
    final lastRefresh = state.lastStateRefreshAt;
    return lastRefresh == null ||
        now.difference(lastRefresh) >= runtimeForegroundRefreshAge;
  }

  bool _canRebootstrap() {
    final last = _lastRebootstrapAt;
    return last == null ||
        DateTime.now().difference(last) >= runtimeRebootstrapCooldown;
  }

  void resetTenant() {
    _revision++;
    _lastRebootstrapAt = null;
    state = const RuntimeAppState();
  }
}

final runtimeControllerProvider =
    NotifierProvider<RuntimeController, RuntimeAppState>(RuntimeController.new);

final runtimeI18nOverridesProvider = Provider<Map<String, String>>(
  (ref) =>
      ref.watch(runtimeControllerProvider).bootstrap?.i18nMessages ?? const {},
);

final runtimeFeatureProvider = Provider.family<bool, String>(
  (ref, key) =>
      ref.watch(runtimeControllerProvider).bootstrap?.hasFeature(key) ?? false,
);

final runtimePermissionProvider = Provider.family<bool, String>(
  (ref, permission) =>
      ref
          .watch(runtimeControllerProvider)
          .bootstrap
          ?.hasPermission(permission) ??
      false,
);
