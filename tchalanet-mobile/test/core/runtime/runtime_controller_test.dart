import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_controller.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_models.dart';
import 'package:tchalanet_mobile/core/runtime/runtime_repository.dart';

class _FakeRuntimeRepository implements RuntimeRepository {
  var publicLoads = 0;
  var tenantLoads = 0;
  final states = <RuntimeStateSnapshot>[];
  bool failTenantLoad = false;

  @override
  Future<RuntimeBootstrap> loadPublic(String locale) async {
    publicLoads++;
    return _bootstrap(RuntimeScope.public, locale: locale);
  }

  @override
  Future<RuntimeBootstrap> loadTenant() async {
    tenantLoads++;
    if (failTenantLoad) throw Exception('tenant bootstrap unavailable');
    return _bootstrap(RuntimeScope.tenant, locale: 'ht');
  }

  @override
  Future<RuntimeStateSnapshot> refreshTenantState() async => states.removeAt(0);
}

RuntimeBootstrap _bootstrap(RuntimeScope scope, {required String locale}) =>
    RuntimeBootstrap(
      scope: scope,
      locale: locale,
      i18nMessages: const {'runtime.title': 'Tit'},
      features: const {'sales.enabled': true},
      roles: const {'CASHIER'},
      permissions: const {'sales.place'},
      readinessStatus: 'READY',
      notifications: const RuntimeNotificationSummary(unreadCount: 2),
      notices: const [],
    );

RuntimeStateSnapshot _snapshot(String version) => RuntimeStateSnapshot(
  status: RuntimeStatus.ready,
  readinessStatus: 'READY',
  notifications: const RuntimeNotificationSummary(unreadCount: 3),
  versions: RuntimeVersions(bootstrap: version),
  notices: const [],
);

void main() {
  test('public bootstrap exposes safe runtime state', () async {
    final repository = _FakeRuntimeRepository();
    final container = ProviderContainer(
      overrides: [runtimeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);

    await container.read(runtimeControllerProvider.notifier).loadPublic('ht');

    final bootstrap = container.read(runtimeControllerProvider).bootstrap!;
    expect(bootstrap.scope, RuntimeScope.public);
    expect(bootstrap.locale, 'ht');
    expect(bootstrap.hasFeature('sales.enabled'), isTrue);
    expect(repository.publicLoads, 1);
  });

  test('changed runtime versions trigger one tenant re-bootstrap', () async {
    final repository = _FakeRuntimeRepository()
      ..states.addAll([_snapshot('v1'), _snapshot('v2')]);
    final container = ProviderContainer(
      overrides: [runtimeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final controller = container.read(runtimeControllerProvider.notifier);

    await controller.loadTenant();
    expect(await controller.refreshTenantState(), RuntimeRefreshOutcome.updated);
    expect(
      await controller.refreshTenantState(),
      RuntimeRefreshOutcome.rebootstrap,
    );
    expect(repository.tenantLoads, 2);
  });

  test('unchanged runtime versions do not reload tenant bootstrap', () async {
    final repository = _FakeRuntimeRepository()
      ..states.addAll([_snapshot('v1'), _snapshot('v1')]);
    final container = ProviderContainer(
      overrides: [runtimeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final controller = container.read(runtimeControllerProvider.notifier);

    await controller.loadTenant();
    await controller.refreshTenantState();
    await controller.refreshTenantState();

    expect(repository.tenantLoads, 1);
  });

  test('failed tenant bootstrap does not retain public runtime gates', () async {
    final repository = _FakeRuntimeRepository();
    final container = ProviderContainer(
      overrides: [runtimeRepositoryProvider.overrideWithValue(repository)],
    );
    addTearDown(container.dispose);
    final controller = container.read(runtimeControllerProvider.notifier);

    await controller.loadPublic('ht');
    repository.failTenantLoad = true;
    await controller.loadTenant();

    expect(container.read(runtimeControllerProvider).bootstrap, isNull);
    expect(container.read(runtimeFeatureProvider('sales.enabled')), isFalse);
    expect(container.read(runtimePermissionProvider('sales.place')), isFalse);
  });
}
