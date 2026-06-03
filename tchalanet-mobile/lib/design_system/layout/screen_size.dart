import 'package:flutter/material.dart';

/// Screen size breakpoints — aligned with Material 3 adaptive layout guidance.
///
/// compact  < 600   → téléphone portrait, terminal POS 5-6"
/// medium   600–959 → téléphone paysage, tablette portrait
/// expanded ≥ 960   → tablette paysage, terminal POS 10"+
enum ScreenSize { compact, medium, expanded }

/// Surface context — separates the *use case* from the raw screen width.
///
/// A POS terminal is often compact in size (5-6" Android kiosk) but needs
/// larger touch targets, higher-contrast typography, and operator-first UX.
/// This enum is set once at app startup from runtime settings and is then
/// propagated via [PosContextProvider].
enum SurfaceContext {
  /// Personal phone — user scrolls, small text OK, standard M3 density.
  mobile,

  /// POS terminal (any size) — operator stands, quick taps, larger targets.
  posTerminal,
}

extension ScreenSizeX on BuildContext {
  ScreenSize get screenSize {
    final w = MediaQuery.sizeOf(this).width;
    if (w < 600) return ScreenSize.compact;
    if (w < 960) return ScreenSize.medium;
    return ScreenSize.expanded;
  }

  bool get isCompact => screenSize == ScreenSize.compact;
  bool get isMedium => screenSize == ScreenSize.medium;
  bool get isExpanded => screenSize == ScreenSize.expanded;

  /// Two-column layout available (tablet landscape ≥ 960 px).
  bool get isPosTerminal => isExpanded;

  /// True for a phone-sized screen (< 600 px).
  bool get isPhone => isCompact;

  /// Minimum touch target height for the current context.
  /// POS: 56 dp — operator fingers, quick taps, no mis-taps.
  /// Mobile: 48 dp — Material 3 default.
  double get minTouchTarget {
    final ctx = PosContextProvider.of(this);
    return ctx == SurfaceContext.posTerminal ? 56.0 : 48.0;
  }

  /// Scale factor applied to action button heights for POS.
  double get posScale {
    final ctx = PosContextProvider.of(this);
    return ctx == SurfaceContext.posTerminal ? 1.15 : 1.0;
  }
}

/// Provides the [SurfaceContext] down the widget tree.
/// Set this at the app root once runtime settings are loaded.
class PosContextProvider extends InheritedWidget {
  const PosContextProvider({
    super.key,
    required this.context,
    required super.child,
  });

  final SurfaceContext context;

  static SurfaceContext of(BuildContext ctx) {
    final provider =
        ctx.dependOnInheritedWidgetOfExactType<PosContextProvider>();
    return provider?.context ?? SurfaceContext.mobile;
  }

  @override
  bool updateShouldNotify(PosContextProvider old) => context != old.context;
}
