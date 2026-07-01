import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';
import { AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { ThemeMode, ThemeStore } from '@tch/ui/theme';

import {
  AdminThemeApi,
  TenantThemeAdminView,
  ThemePresetView,
} from './admin-theme-api.service';

type PageState = 'loading' | 'ready' | 'error';

interface ModeOption {
  readonly value: ThemeMode;
  readonly labelKey: string;
  readonly icon: string;
}

const MODE_OPTIONS: readonly ModeOption[] = [
  { value: 'light', labelKey: 'admin.appearance.mode.light', icon: 'light_mode' },
  { value: 'dark', labelKey: 'admin.appearance.mode.dark', icon: 'dark_mode' },
  { value: 'system', labelKey: 'admin.appearance.mode.system', icon: 'contrast' },
];

@Component({
  selector: 'tch-admin-appearance-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    TranslatePipe,
    MatButtonModule,
    MatIconModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchLoading,
    TchErrorPanel,
    TchNotice,
  ],
  templateUrl: './admin-appearance.page.html',
  styleUrl: './admin-appearance.page.scss',
})
export class AdminAppearancePage implements OnInit {
  private readonly api = inject(AdminThemeApi);
  private readonly theme = inject(ThemeStore);
  private readonly translate = inject(TranslateService);

  protected readonly modeOptions = MODE_OPTIONS;

  readonly pageState = signal<PageState>('loading');
  readonly current = signal<TenantThemeAdminView | null>(null);
  readonly presets = signal<readonly ThemePresetView[]>([]);
  readonly selectedMode = signal<ThemeMode>('light');

  readonly savingPreset = signal<string | null>(null);
  readonly savingMode = signal(false);
  readonly resetting = signal(false);

  readonly successKey = signal<string | null>(null);
  readonly errorKey = signal<string | null>(null);
  // True once the backend reports the tenant plan does not include preset selection.
  readonly featureLocked = signal(false);

  readonly loading = computed(() => this.pageState() === 'loading');
  readonly activePresetCode = computed(() => this.current()?.presetCode ?? null);
  readonly activePresets = computed(() => this.presets().filter(preset => preset.active));
  readonly selectedPreset = computed(() =>
    this.presets().find(preset => preset.code === this.activePresetCode()) ?? null,
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.pageState.set('loading');
    this.successKey.set(null);
    this.errorKey.set(null);

    this.api.get({ suppressShellFeedback: true }).subscribe({
      next: view => {
        this.current.set(view);
        this.selectedMode.set(normalizeMode(view?.defaultMode));
        this.previewCurrent();
        this.pageState.set('ready');
      },
      error: () => this.pageState.set('error'),
    });

    this.api.listPresets({ suppressShellFeedback: true }).subscribe({
      next: presets => this.presets.set(presets),
      error: () => this.presets.set([]),
    });
  }

  selectPreset(preset: ThemePresetView): void {
    if (this.savingPreset() || !preset.active || preset.code === this.activePresetCode()) {
      return;
    }
    this.successKey.set(null);
    this.errorKey.set(null);
    this.savingPreset.set(preset.code);

    this.api.applyPreset(preset.code, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.current.update(c =>
          c ? { ...c, presetCode: preset.code, isDefault: preset.isDefault } : createTenantThemeView(preset, this.selectedMode()),
        );
        this.theme.setPreset(preset.code);
        this.savingPreset.set(null);
        this.successKey.set('admin.appearance.feedback.presetSaved');
      },
      error: (err: unknown) => {
        this.savingPreset.set(null);
        if (problemCode(err) === 'entitlement.feature_required') {
          this.featureLocked.set(true);
        } else {
          this.errorKey.set('admin.appearance.error.save');
        }
      },
    });
  }

  selectMode(mode: ThemeMode): void {
    if (this.savingMode() || mode === this.selectedMode()) {
      return;
    }
    this.successKey.set(null);
    this.errorKey.set(null);
    this.savingMode.set(true);

    const previous = this.selectedMode();
    this.selectedMode.set(mode);
    this.theme.setMode(mode);

    this.api.updateMode(mode, { suppressShellFeedback: true }).subscribe({
      next: () => {
        this.current.update(c => (c ? { ...c, defaultMode: mode } : c));
        this.savingMode.set(false);
        this.successKey.set('admin.appearance.feedback.modeSaved');
      },
      error: () => {
        this.savingMode.set(false);
        this.selectedMode.set(previous);
        this.theme.setMode(previous);
        this.errorKey.set('admin.appearance.error.save');
      },
    });
  }

  resetTheme(): void {
    if (this.resetting()) {
      return;
    }
    this.successKey.set(null);
    this.errorKey.set(null);
    this.resetting.set(true);

    this.api.reset({ suppressShellFeedback: true }).subscribe({
      next: () => {
        this.resetting.set(false);
        this.featureLocked.set(false);
        this.successKey.set('admin.appearance.feedback.reset');
        this.load();
      },
      error: () => {
        this.resetting.set(false);
        this.errorKey.set('admin.appearance.error.save');
      },
    });
  }

  presetLabel(preset: ThemePresetView): string {
    if (!preset.labelKey) {
      return preset.code;
    }
    const translated = this.translate.instant(preset.labelKey);
    return translated === preset.labelKey ? preset.code : translated;
  }

  presetVendor(preset: ThemePresetView): string {
    return preset.vendor || 'System';
  }

  presetColor(preset: ThemePresetView, token: string, mode: 'light' | 'dark' = 'light'): string {
    return preset.config?.tokens?.[mode]?.[token] ?? fallbackPresetColor(preset.code, token, mode);
  }

  private previewCurrent(): void {
    const code = this.activePresetCode();
    if (code) {
      this.theme.setPreset(code);
    }
    this.theme.setMode(this.selectedMode());
  }
}

function normalizeMode(value: string | null | undefined): ThemeMode {
  const v = (value ?? '').toLowerCase();
  return v === 'dark' || v === 'system' ? v : 'light';
}

function createTenantThemeView(preset: ThemePresetView, mode: ThemeMode): TenantThemeAdminView {
  return {
    presetCode: preset.code,
    defaultMode: mode,
    active: true,
    isDefault: preset.isDefault,
    version: 0,
    updatedAt: new Date().toISOString(),
  };
}

function fallbackPresetColor(code: string, token: string, mode: 'light' | 'dark'): string {
  const fallback = FALLBACK_THEME_COLORS[Math.abs(hashCode(code)) % FALLBACK_THEME_COLORS.length];
  if (token === 'color.primary') {
    return mode === 'dark' ? fallback.darkPrimary : fallback.primary;
  }
  if (token === 'color.secondary') {
    return fallback.secondary;
  }
  if (token === 'color.onSurface') {
    return mode === 'dark' ? '#e6e0e9' : '#1c1b1f';
  }
  return mode === 'dark' ? '#141218' : '#fffbfe';
}

function hashCode(value: string): number {
  return Array.from(value).reduce((hash, char) => (hash * 31 + char.charCodeAt(0)) | 0, 0);
}

const FALLBACK_THEME_COLORS = [
  { primary: '#006874', secondary: '#4a6267', darkPrimary: '#4fd8e8' },
  { primary: '#6750a4', secondary: '#625b71', darkPrimary: '#d0bcff' },
  { primary: '#0061a4', secondary: '#535f70', darkPrimary: '#9ecaff' },
  { primary: '#006e1c', secondary: '#52634f', darkPrimary: '#72dd68' },
  { primary: '#ba1a1a', secondary: '#775652', darkPrimary: '#ffb4ab' },
  { primary: '#8b5000', secondary: '#715a41', darkPrimary: '#ffb870' },
];

function problemCode(err: unknown): string | undefined {
  return (err as { error?: ProblemDetail })?.error?.code;
}
