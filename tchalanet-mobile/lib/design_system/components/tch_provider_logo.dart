import 'package:flutter/material.dart';

import '../tokens/tch_radius.dart';

/// Compact provider identity shared by cashier surfaces.
///
/// The source assets are the same official lottery logos used by the web
/// console. A code mark remains available for providers without a supplied
/// logo, so missing artwork never removes the draw identity from the POS.
class TchProviderLogo extends StatelessWidget {
  const TchProviderLogo({
    super.key,
    required this.providerCode,
    this.size = 36,
  });

  final String? providerCode;
  final double size;

  static const _assets = <String, String>{
    'CA': 'assets/images/providers/calottery-logo.png',
    'FL': 'assets/images/providers/florida-lottery.jpeg',
    'GA': 'assets/images/providers/ga_logo.png',
    'IL': 'assets/images/providers/illinois-logo.png',
    'MI': 'assets/images/providers/mi_logo.webp',
    'MS': 'assets/images/providers/msl-logo.png',
    'NJ': 'assets/images/providers/nj_lottery_logo.png',
    'NY': 'assets/images/providers/ny_logo.png',
    'OH': 'assets/images/providers/oh_logo.png',
    'PA': 'assets/images/providers/pa-lottery-logo.png',
    'TX': 'assets/images/providers/tx_logo.png',
  };

  /// Infers the provider from current display labels returned by ticket APIs.
  /// Prefer an explicit provider code whenever the endpoint supplies one.
  static String? providerCodeFromLabel(String? label) {
    final normalized = label?.toUpperCase() ?? '';
    for (final entry in _providerNames.entries) {
      if (normalized.contains(entry.key)) return entry.value;
    }
    return null;
  }

  static const _providerNames = <String, String>{
    'CALIFORNIA': 'CA',
    'FLORIDA': 'FL',
    'GEORGIA': 'GA',
    'ILLINOIS': 'IL',
    'MICHIGAN': 'MI',
    'MISSISSIPPI': 'MS',
    'NEW JERSEY': 'NJ',
    'NEW YORK': 'NY',
    'OHIO': 'OH',
    'PENNSYLVANIA': 'PA',
    'TEXAS': 'TX',
  };

  @override
  Widget build(BuildContext context) {
    final code = providerCode?.trim().toUpperCase();
    final asset = code == null ? null : _assets[code];
    final scheme = Theme.of(context).colorScheme;

    return Semantics(
      label: code == null ? 'Provider' : 'Provider $code',
      image: asset != null,
      child: Container(
        width: size,
        height: size,
        padding: EdgeInsets.all(size * 0.1),
        decoration: BoxDecoration(
          color: scheme.surfaceContainerLowest,
          border: Border.all(color: scheme.outlineVariant),
          borderRadius: BorderRadius.circular(TchRadius.sm),
        ),
        child: asset == null
            ? _ProviderCodeMark(code: code ?? '--')
            : Image.asset(
                asset,
                fit: BoxFit.contain,
                errorBuilder: (_, _, _) => _ProviderCodeMark(code: code!),
              ),
      ),
    );
  }
}

class _ProviderCodeMark extends StatelessWidget {
  const _ProviderCodeMark({required this.code});

  final String code;

  @override
  Widget build(BuildContext context) => Center(
    child: Text(
      code,
      style: Theme.of(context).textTheme.labelSmall?.copyWith(
        color: Theme.of(context).colorScheme.primary,
        fontWeight: FontWeight.w900,
      ),
    ),
  );
}
