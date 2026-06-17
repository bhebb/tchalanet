import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformSettingsApi,
  SettingView,
  SettingsCatalogStatsView,
  SettingLevel,
} from '../../platform-settings-api.service';
import { CreateSettingDialog, SETTING_LEVELS } from './dialogs/create-setting.dialog';
import { EditSettingDialog } from './dialogs/edit-setting.dialog';
import { DeleteSettingDialog } from './dialogs/delete-setting.dialog';

@Component({
  selector: 'tch-platform-ops-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-ops-settings.page.html',
  styleUrls: ['./platform-ops-settings.page.scss'],
})
export class PlatformOpsSettingsPage implements OnInit {
  private readonly api = inject(PlatformSettingsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['namespace', 'settingKey', 'settingValue', 'valueType', 'level', 'exposure', 'active', 'actions'];
  readonly levels = SETTING_LEVELS;

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
        this.settings.set(p.content);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onKeyFilter(v: string): void { this.settingKeyFilter.set(v); this.page.set(0); this.load(); }
  onNamespace(e: Event): void { this.namespace.set((e.target as HTMLInputElement).value); this.page.set(0); this.load(); }
  onLevelFilter(v: string): void { this.levelFilter.set(v); this.page.set(0); this.load(); }
  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  openCreate(): void {
    const ref = this.dialog.open(CreateSettingDialog, { width: '560px' });
    ref.afterClosed().subscribe((created: SettingView | null) => {
      if (created) {
        this.snackBar.open('Paramètre créé.', 'OK', { duration: 4000 });
        this.load();
        this.api.getOverview().subscribe({ next: s => this.stats.set(s) });
      }
    });
  }

  openEdit(setting: SettingView): void {
    const ref = this.dialog.open(EditSettingDialog, { data: setting, width: '520px' });
    ref.afterClosed().subscribe((updated: SettingView | null) => {
      if (updated) { this.snackBar.open('Paramètre mis à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openDelete(setting: SettingView): void {
    const ref = this.dialog.open(DeleteSettingDialog, { data: setting, width: '420px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.api.deleteSetting(setting.id.value).subscribe({
        next: () => {
          this.snackBar.open('Paramètre supprimé.', 'OK', { duration: 4000 });
          this.load();
          this.api.getOverview().subscribe({ next: s => this.stats.set(s) });
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.snackBar.open(pd?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
        },
      });
    });
  }
}
