import { describe, expect, it } from 'vitest';

import { mapBackendThemeTokens } from './theme-token-map';

describe('mapBackendThemeTokens', () => {
  it('maps dotted backend token keys to validated --tch-* CSS variables', () => {
    const mapped = mapBackendThemeTokens({
      'color.primary': '#006874',
      'color.secondary': '#4A6267',
      'color.surface': '#FFFBFE',
      'color.surfaceContainerLowest': '#FFFFFF',
      'color.onSurface': '#1C1B1F',
      'color.onSurfaceVariant': '#464652',
      'color.secondaryContainer': '#FECB00',
      'color.statusReady': '#10B981',
      'shape.radius.md': '12px',
    });

    expect(mapped).toEqual({
      '--tch-color-primary': '#006874',
      '--tch-color-secondary': '#4A6267',
      '--tch-color-surface': '#FFFBFE',
      '--tch-color-surface-container-lowest': '#FFFFFF',
      '--tch-color-on-surface': '#1C1B1F',
      '--tch-color-on-surface-variant': '#464652',
      '--tch-color-secondary-container': '#FECB00',
      '--tch-color-status-ready': '#10B981',
      '--tch-radius-md': '12px',
    });
  });

  it('drops unmapped backend tokens instead of emitting invalid CSS variables', () => {
    const mapped = mapBackendThemeTokens({
      'color.primary': '#006874',
      'typography.fontFamily': 'roboto',
      'density.default': 'comfortable',
    });

    expect(mapped).toEqual({
      '--tch-color-primary': '#006874',
      '--tch-font-family': 'roboto',
    });
    expect(mapped['--color.primary']).toBeUndefined();
  });

  it('passes through keys already expressed as --tch-* custom properties', () => {
    const mapped = mapBackendThemeTokens({
      '--tch-color-primary': '#123456',
      '--tch-radius-control': '8px',
    });

    expect(mapped).toEqual({
      '--tch-color-primary': '#123456',
      '--tch-radius-control': '8px',
    });
  });

  it('returns an empty map when given no tokens', () => {
    expect(mapBackendThemeTokens({})).toEqual({});
  });
});
