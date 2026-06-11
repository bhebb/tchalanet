import 'package:flutter/material.dart';

import 'runtime_theme.dart';
import 'theme_builder.dart';

abstract final class TchTheme {
  static ThemeData light() =>
      ThemeBuilder.buildFromTokens(RuntimeTheme.defaultTheme.tokens);
}
