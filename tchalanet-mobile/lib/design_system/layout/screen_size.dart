import 'package:flutter/material.dart';

/// Screen size breakpoints — aligned with Material 3 adaptive layout guidance.
///
/// compact  < 600   → téléphone portrait, petite tablette
/// medium   600–959 → téléphone paysage, tablette portrait
/// expanded ≥ 960   → tablette paysage, terminal POS 10"
enum ScreenSize { compact, medium, expanded }

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

  /// True for terminal POS (tablette paysage ≥ 960 px).
  bool get isPosTerminal => isExpanded;

  /// True pour un téléphone ou une petite surface (< 600 px).
  bool get isPhone => isCompact;
}
