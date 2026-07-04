import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Coarse device network reachability.
///
/// Reports whether the device has at least one active network interface.
/// This is a connectivity *hint* (an interface is up) — it does not guarantee
/// the Tchalanet API is reachable. V1 is online-only (no offline sync); this
/// only drives the POS network indicator so a seller who loses 3G sees it.
abstract interface class ConnectivityRepository {
  /// Emits the current reachability immediately, then on every change.
  /// `true` = online, `false` = fully offline.
  Stream<bool> watch();
}

class ConnectivityRepositoryImpl implements ConnectivityRepository {
  ConnectivityRepositoryImpl(this._connectivity);

  final Connectivity _connectivity;

  @override
  Stream<bool> watch() async* {
    yield _isOnline(await _connectivity.checkConnectivity());
    yield* _connectivity.onConnectivityChanged.map(_isOnline);
  }

  static bool _isOnline(List<ConnectivityResult> results) =>
      results.any((r) => r != ConnectivityResult.none);
}

final connectivityRepositoryProvider = Provider<ConnectivityRepository>(
  (_) => ConnectivityRepositoryImpl(Connectivity()),
);

/// Device online state. Defaults to `true` (optimistic) while the first
/// reading resolves, so the UI never flashes "offline" on cold start.
final isOnlineProvider = StreamProvider<bool>(
  (ref) => ref.watch(connectivityRepositoryProvider).watch(),
);
