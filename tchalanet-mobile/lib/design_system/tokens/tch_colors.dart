import 'package:flutter/material.dart';

abstract final class TchColors {
  static const primary = Color(0xFF1A1B4B);
  static const primaryStrong = Color(0xFF141545);
  static const onPrimary = Color(0xFFFFFFFF);
  static const onPrimarySoft = Color(0xFFF2EFFF);
  static const primaryContainer = Color(0xFF2E3192);
  static const onPrimaryContainer = Color(0xFFFFFFFF);
  static const primaryFixed = Color(0xFFE1E0FF);
  static const primaryFixedDim = Color(0xFFC1C1FC);
  static const onPrimaryFixed = Color(0xFF141545);
  static const onPrimaryFixedVariant = Color(0xFF404273);

  static const secondary = Color(0xFF5D5C72);
  static const onSecondary = Color(0xFFFFFFFF);
  static const secondaryContainer = Color(0xFFE2E0FA);
  static const onSecondaryContainer = Color(0xFF191A2D);
  static const secondaryFixed = Color(0xFFE2E0FA);
  static const secondaryFixedDim = Color(0xFFC6C4DE);
  static const onSecondaryFixed = Color(0xFF191A2D);
  static const onSecondaryFixedVariant = Color(0xFF45455A);

  static const tertiary = Color(0xFFFECB00);
  static const onTertiary = Color(0xFF241A00);
  static const tertiaryContainer = Color(0xFFFFE08B);
  static const onTertiaryContainer = Color(0xFF241A00);
  static const tertiaryFixed = Color(0xFFFFE08B);
  static const tertiaryFixedDim = Color(0xFFF1C100);
  static const onTertiaryFixed = Color(0xFF241A00);
  static const onTertiaryFixedVariant = Color(0xFF584400);

  static const background = Color(0xFFF9F9FC);
  static const surface = Color(0xFFF9F9FC);
  static const surfaceBright = Color(0xFFFFFFFF);
  static const surfaceDim = Color(0xFFDCD9DE);
  static const surfaceContainerLowest = Color(0xFFFFFFFF);
  static const surfaceContainerLow = Color(0xFFF6F2F7);
  static const surfaceContainer = Color(0xFFF0EDF2);
  static const surfaceContainerHigh = Color(0xFFEAE7EC);
  static const surfaceContainerHighest = Color(0xFFE5E1E6);
  static const surfaceTint = Color(0xFF1A1B4B);

  static const onSurface = Color(0xFF1A1C1E);
  static const onSurfaceVariant = Color(0xFF464652);
  static const outline = Color(0xFF777680);
  static const outlineVariant = Color(0xFFC8C5D0);
  static const outlineStrong = Color(0xFF5E5D67);
  static const inverseSurface = Color(0xFF313034);
  static const onInverseSurface = Color(0xFFF3EFF4);
  static const inversePrimary = Color(0xFFC1C1FC);
  static const shadow = Color(0xFF000000);
  static const scrim = Color(0xFF000000);

  static const online = Color(0xFF22C55E); // live terminal indicator
  static const success = Color(0xFF006C49);
  static const successContainer = Color(0xFFDDFBEA);
  static const warning = Color(0xFFB26A00);
  static const warningContainer = Color(0xFFFFF2D6);
  static const missing = outlineStrong;
  static const blocked = error;
  static const error = Color(0xFFBA1A1A);
  static const onError = Color(0xFFFFFFFF);
  static const errorContainer = Color(0xFFFFDAD6);
  static const onErrorContainer = Color(0xFF410002);
}
