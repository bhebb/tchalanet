import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminMetricCardComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  PlatformSettingsApi,
  SettingView,
  SettingsCatalogStatsView,
  SettingLevel,
  SettingExposure,
  UpdateSettingRequest,
} from '../../data-access/platform-settings-api.service';
import { CreateSettingDialog, EXPOSURES, SETTING_LEVELS } from '../../components/dialogs/create-setting.dialog';
import { DeleteSettingDialog } from '../../components/dialogs/delete-setting.dialog';

@Component({
  selector: 'tch-platform-catalog-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminMetricCardComponent,
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-settings.page.html',
  styleUrls: ['./platform-catalog-settings.page.scss'],
})
export class PlatformCatalogSettingsPage implements OnInit {
  private readonly api = inject(PlatformSettingsApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['namespace', 'settingKey', 'settingValue', 'valueType', 'level', 'exposure', 'active', 'actions'];
  readonly levels = SETTING_LEVELS;
  readonly exposures = EXPOSURES;

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly settings = signal<SettingView[]>([]);
  readonly stats = signal<SettingsCatalogStatsView | null>(null);
  readonly settingKeyFilter = signal('');
  readonly namespace = signal('');
  readonly levelFilter = signal('');
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly selectedSetting = signal<SettingView | null>(null);
  readonly savingConfig = signal(false);

  readonly configForm = this.fb.nonNullable.group({
    settingValue: ['', Validators.required],
    exposure: ['INTERNAL' as SettingExposure],
    active: [true],
  });

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void {
    this.load();
    this.api.getOverview().subscribe({ next: s => this.stats.set(s) });
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listSettings({
      namespace: this.namespace() || undefined,
      settingKey: this.settingKeyFilter() || undefined,
      level: (this.levelFilter() as SettingLevel) || undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.settings.set(p.items);
        this.totalElements.set(p.totalElements);
        this.page.set(p.page);
        this.totalPages.set(p.totalPages || 1);
        this.hasNext.set(p.hasNext ?? false);
        this.hasPrevious.set(p.hasPrevious ?? false);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onKeyFilter(v: string): void { this.settingKeyFilter.set(v); this.page.set(0); this.load(); }
  onNamespace(e: Event): void { this.namespace.set((e.target as HTMLInputElement).value); this.page.set(0); this.load(); }
  onLevelFilter(v: string): void { this.levelFilter.set(v); this.page.set(0); this.load(); }
  prevPage(): void { if (this.hasPrevious()) { this.page.set(this.page() - 1); this.load(); } }
  nextPage(): void { if (this.hasNext()) { this.page.set(this.page() + 1); this.load(); } }

  openCreate(): void {
    const ref = this.dialog.open(CreateSettingDialog, { width: '560px' });
    ref.afterClosed().subscribe((created: SettingView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Paramètre créé.', 'OK', { duration: 4000 }); this.load(); this.api.getOverview().subscribe({ next: s => this.stats.set(s) }); }
    });
  }

  openConfig(setting: SettingView): void {
    this.selectedSetting.set(setting);
    this.configForm.reset({
      settingValue: setting.settingValue,
      exposure: setting.exposure,
      active: setting.active,
    });
  }

  closeConfig(): void {
    this.selectedSetting.set(null);
  }

  saveConfig(): void {
    const setting = this.selectedSetting();
    if (!setting || this.configForm.invalid || this.savingConfig()) return;

    this.savingConfig.set(true);
    const v = this.configForm.getRawValue();
    const req: UpdateSettingRequest = {
      settingValue: v.settingValue,
      exposure: v.exposure,
      active: v.active,
    };
    this.api.updateSetting(setting.id, req).subscribe({
      next: updated => {
        this.savingConfig.set(false);
        this.snackBar.open('Paramètre mis à jour.', 'OK', { duration: 4000 });
        this.openConfig(updated);
        this.load();
      },
      error: (err: unknown) => {
        this.savingConfig.set(false);
        this.showError((err as { error?: { detail?: string; title?: string } })?.error?.detail
          ?? (err as { error?: { title?: string } })?.error?.title
          ?? 'Erreur.');
      },
    });
  }

  openDelete(setting: SettingView): void {
    const ref = this.dialog.open(DeleteSettingDialog, { data: setting, width: '420px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.api.deleteSetting(setting.id).subscribe({
        next: () => {
          this.snackBar.open('Paramètre supprimé.', 'OK', { duration: 4000 });
          this.load();
          this.api.getOverview().subscribe({ next: s => this.stats.set(s) });
        },
        error: (err: unknown) => {
          this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
        },
      });
    });
  }
}
