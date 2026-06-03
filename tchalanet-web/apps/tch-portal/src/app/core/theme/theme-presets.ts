import { ThemePreset } from '../../shared/types';

export const defaultThemePresetId = 'tchalanet';

export const fallbackThemePresets: readonly ThemePreset[] = [
  {
    id: defaultThemePresetId,
    labelKey: 'theme.presets.tchalanet',
    css: `
      .tch-theme[data-preset='tchalanet'] {
        --tch-color-background: #f7f8fb;
        --tch-color-foreground: #16202a;
        --tch-color-primary: #0c7c59;
        --tch-color-primary-contrast: #ffffff;
        --tch-color-secondary: #2458d3;
        --tch-color-surface: #ffffff;
        --tch-color-surface-muted: #edf2f7;
        --tch-color-outline: #c9d4df;
        --tch-radius-control: 6px;
        --mat-sys-primary: var(--tch-color-primary);
        --mat-sys-on-primary: var(--tch-color-primary-contrast);
        --mat-sys-secondary: var(--tch-color-secondary);
        --mat-sys-surface: var(--tch-color-surface);
        --mat-sys-background: var(--tch-color-background);
        --mat-sys-on-surface: var(--tch-color-foreground);
        --mat-sys-outline: var(--tch-color-outline);
        color-scheme: light;
      }

      .tch-theme.dark[data-preset='tchalanet'] {
        --tch-color-background: #111827;
        --tch-color-foreground: #f8fafc;
        --tch-color-primary: #34d399;
        --tch-color-primary-contrast: #052e23;
        --tch-color-secondary: #93c5fd;
        --tch-color-surface: #1f2937;
        --tch-color-surface-muted: #243244;
        --tch-color-outline: #475569;
        color-scheme: dark;
      }
    `,
  },
  {
    id: 'material-indigo',
    labelKey: 'theme.presets.materialIndigo',
    css: `
      .tch-theme[data-preset='material-indigo'] {
        --tch-color-background: #f8f7ff;
        --tch-color-foreground: #1b1b21;
        --tch-color-primary: #3f51b5;
        --tch-color-primary-contrast: #ffffff;
        --tch-color-secondary: #c2185b;
        --tch-color-surface: #ffffff;
        --tch-color-surface-muted: #eef0ff;
        --tch-color-outline: #c8c5d8;
        --tch-radius-control: 6px;
        --mat-sys-primary: var(--tch-color-primary);
        --mat-sys-on-primary: var(--tch-color-primary-contrast);
        --mat-sys-secondary: var(--tch-color-secondary);
        --mat-sys-surface: var(--tch-color-surface);
        --mat-sys-background: var(--tch-color-background);
        --mat-sys-on-surface: var(--tch-color-foreground);
        --mat-sys-outline: var(--tch-color-outline);
        color-scheme: light;
      }

      .tch-theme.dark[data-preset='material-indigo'] {
        --tch-color-background: #12131f;
        --tch-color-foreground: #f8f7ff;
        --tch-color-primary: #b8c4ff;
        --tch-color-primary-contrast: #15194a;
        --tch-color-secondary: #ffb0cc;
        --tch-color-surface: #1b1c2b;
        --tch-color-surface-muted: #25273a;
        --tch-color-outline: #4a4d65;
        color-scheme: dark;
      }
    `,
  },
  {
    id: 'material-green',
    labelKey: 'theme.presets.materialGreen',
    css: `
      .tch-theme[data-preset='material-green'] {
        --tch-color-background: #f4fbf6;
        --tch-color-foreground: #172018;
        --tch-color-primary: #2e7d32;
        --tch-color-primary-contrast: #ffffff;
        --tch-color-secondary: #006c9c;
        --tch-color-surface: #ffffff;
        --tch-color-surface-muted: #e8f3eb;
        --tch-color-outline: #bdd1c2;
        --tch-radius-control: 6px;
        --mat-sys-primary: var(--tch-color-primary);
        --mat-sys-on-primary: var(--tch-color-primary-contrast);
        --mat-sys-secondary: var(--tch-color-secondary);
        --mat-sys-surface: var(--tch-color-surface);
        --mat-sys-background: var(--tch-color-background);
        --mat-sys-on-surface: var(--tch-color-foreground);
        --mat-sys-outline: var(--tch-color-outline);
        color-scheme: light;
      }

      .tch-theme.dark[data-preset='material-green'] {
        --tch-color-background: #101811;
        --tch-color-foreground: #f1fbf3;
        --tch-color-primary: #92d98e;
        --tch-color-primary-contrast: #0c2b0d;
        --tch-color-secondary: #8bd0ef;
        --tch-color-surface: #19231a;
        --tch-color-surface-muted: #223024;
        --tch-color-outline: #435746;
        color-scheme: dark;
      }
    `,
  },
];
