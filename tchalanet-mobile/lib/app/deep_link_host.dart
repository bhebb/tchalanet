import 'dart:async';

import 'package:app_links/app_links.dart';
import 'package:flutter/widgets.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../features/auth/presentation/view_models/auth_controller.dart';
import 'deep_link_parser.dart';

class PendingTicketVerificationCodeController extends Notifier<String?> {
  @override
  String? build() => null;

  void setCode(String code) => state = code;

  String? take() {
    final code = state;
    state = null;
    return code;
  }
}

final pendingTicketVerificationCodeProvider =
    NotifierProvider<PendingTicketVerificationCodeController, String?>(
      PendingTicketVerificationCodeController.new,
    );

class DeepLinkHost extends ConsumerStatefulWidget {
  const DeepLinkHost({required this.child, super.key});

  final Widget child;

  @override
  ConsumerState<DeepLinkHost> createState() => _DeepLinkHostState();
}

class _DeepLinkHostState extends ConsumerState<DeepLinkHost> {
  late final AppLinks _appLinks;
  StreamSubscription<Uri>? _subscription;

  @override
  void initState() {
    super.initState();
    _appLinks = AppLinks();
    _bindAuthRedirect();
    _listenForLinks();
  }

  void _bindAuthRedirect() {
    ref.listenManual(authControllerProvider, (_, next) {
      if (next is AuthAuthenticated) {
        _routePendingCode();
      }
    });
  }

  Future<void> _listenForLinks() async {
    try {
      final initial = await _appLinks.getInitialLink();
      if (!mounted) return;
      _handleLink(initial);
    } catch (_) {
      // Ignore malformed platform link payloads; manual verification remains available.
    }
    _subscription = _appLinks.uriLinkStream.listen(
      _handleLink,
      onError: (_) {
        // Ignore stream errors; the app can still verify manually.
      },
    );
  }

  void _handleLink(Uri? uri) {
    final code = publicCodeFromTicketVerificationUri(uri);
    if (code == null) return;

    final auth = ref.read(authControllerProvider);
    if (auth is AuthAuthenticated) {
      _goToScan(code);
      return;
    }

    ref.read(pendingTicketVerificationCodeProvider.notifier).setCode(code);
    if (mounted) {
      context.go('/login');
    }
  }

  void _routePendingCode() {
    final code = ref
        .read(pendingTicketVerificationCodeProvider.notifier)
        .take();
    if (code == null || code.isBlank) return;
    _goToScan(code);
  }

  void _goToScan(String code) {
    if (!mounted) return;
    context.go('/pos/scan?code=${Uri.encodeQueryComponent(code)}');
  }

  @override
  void dispose() {
    _subscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => widget.child;
}

extension on String {
  bool get isBlank => trim().isEmpty;
}
