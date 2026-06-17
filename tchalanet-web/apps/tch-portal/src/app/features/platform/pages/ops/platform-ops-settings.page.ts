import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import {
  MAT_DIALOG_DATA,
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../private/shared/admin-ui/admin-crud-shell.component';
import { AdminDataToolbarComponent } from '../../../private/shared/admin-ui/admin-data-toolbar.component';
import { AdminEmptyStateComponent } from '../../../private/shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import {
  PlatformSettingsApi,
  SettingView,
  SettingsCatalogStatsView,
} from '../../platform-settings-api.service';

// ── Edit Setting Dialog ────────────────────────────────────────────────────────

@Component({
  selector: 'tch-edit-setting-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Modifier un paramètre</h2>
    <mat-dialog-content>
      <div class="readonly-info">
        <span class="readonly-label">Namespace</span>
        <span class="readonly-value">{{ data.namespace }}</span>
        <span class="readonly-label">Clé</span>
        <span class="readonly-value mono">{{ data.settingKey }}</span>
        <span class="readonly-label">Type</span>
        <span class="readonly-value">{{ data.valueType }}</span>
      </div>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="settingValue" rows="4"></textarea>
          @if (form.controls['settingValue'].invalid && form.controls['settingValue'].touched) {
            <mat-error>Valeur requise.</mat-error>
          }
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">
          <span class="material-symbols-outlined">error</span>
          {{ error() }}
        </div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) {
          <span class="material-symbols-outlined spin">progress_activity</span>
        }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .readonly-info {
        display: grid;
        grid-template-columns: auto 1fr;
        gap: 0.25rem 0.75rem;
        margin-bottom: 1rem;
        font-size: 0.875rem;
      }
      .readonly-label { color: var(--tch-color-on-surface-variant); font-weight: 500; }
      .readonly-value { color: var(--tch-color-on-surface); }
      .mono { font-family: monospace; }
      .dialog-form { min-width: 400px; }
      .full-width { width: 100%; }
      .error-panel {
        display: flex; align-items: center; gap: 0.5rem; padding: 0.75rem;
        border-radius: 0.5rem; background: var(--tch-color-error-container);
        color: var(--tch-color-on-error-container); font-size: 0.875rem; margin-top: 0.5rem;
      }
      .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
      @keyframes spin { to { transform: rotate(360deg); } }
    `,
  ],
})
export class EditSettingDialog {
  protected readonly data = inject<SettingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditSettingDialog>);
  private readonly api = inject(PlatformSettingsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    settingValue: [this.data.settingValue, Validators.required],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateSetting(this.data.id.value, { settingValue: this.form.value.settingValue! }).subscribe({
      next: updated => {
        this.submitting.set(false);
        this.dialogRef.close(updated);
      },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la mise à jour.');
      },
    });
  }
}

// ── Delete Confirm Dialog ──────────────────────────────────────────────────────

@Component({
  selector: 'tch-delete-setting-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatDialogModule, MatIconModule],
  template: `
    <h2 mat-dialog-title>Supprimer le paramètre</h2>
    <mat-dialog-content>
      <p>Voulez-vous vraiment supprimer <strong class="mono">{{ data.settingKey }}</strong> ?</p>
      <p class="warning-text">Cette action est irréversible.</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Supprimer</button>
    </mat-dialog-actions>
  `,
  styles: [
    `
      .mono { font-family: monospace; }
      .warning-text { color: var(--tch-color-error); font-size: 0.875rem; }
    `,
  ],
})
export class DeleteSettingDialog {
  protected readonly data = inject<SettingView>(MAT_DIALOG_DATA);
}

// ── Main Page ─────────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-platform-ops-settings-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    TchErrorPanel,
    TchLoading,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  template: `
    <tch-admin-page-shell title="Paramètres de la plateforme" description="Consultez et modifiez les paramètres de configuration.">
      @if (stats()) {
        <div class="kpi-row">
          <div class="kpi-chip">
            <span class="kpi-value">{{ stats()!.totalKeys }}</span>
            <span class="kpi-label">Clés</span>
          </div>
        </div>
      }

      <tch-admin-crud-shell>
        <div toolbar>
          <tch-admin-data-toolbar
            searchPlaceholder="Rechercher..."
            [searchValue]="search()"
            (searchChange)="onSearch($event)"
          >
            <mat-form-field appearance="outline" style="min-width:180px">
              <mat-label>Namespace</mat-label>
              <input matInput [value]="namespace()" (input)="onNamespace($event)" placeholder="ex: catalog" />
            </mat-form-field>
            <button mat-stroked-button (click)="load()">
              <span class="material-symbols-outlined">refresh</span>
            </button>
          </tch-admin-data-toolbar>
        </div>

        <div content>
          @if (loading()) {
            <tch-loading label="Chargement..." />
          } @else if (error()) {
            <tch-error-panel [title]="error()!" [showRetry]="true" retryLabel="Réessayer" (retry)="load()" />
          } @else if (settings().length === 0) {
            <tch-admin-empty-state icon="tune" title="Aucun paramètre" message="Aucun paramètre trouvé pour ce filtre." />
          } @else {
            <div class="table-container">
              <table mat-table [dataSource]="settings()">
                <ng-container matColumnDef="namespace">
                  <th mat-header-cell *matHeaderCellDef>Namespace</th>
                  <td mat-cell *matCellDef="let row">{{ row.namespace }}</td>
                </ng-container>
                <ng-container matColumnDef="settingKey">
                  <th mat-header-cell *matHeaderCellDef>Clé</th>
                  <td mat-cell *matCellDef="let row"><span style="font-family:monospace">{{ row.settingKey }}</span></td>
                </ng-container>
                <ng-container matColumnDef="settingValue">
                  <th mat-header-cell *matHeaderCellDef>Valeur</th>
                  <td mat-cell *matCellDef="let row">{{ row.settingValue.length > 40 ? row.settingValue.slice(0, 40) + '…' : row.settingValue }}</td>
                </ng-container>
                <ng-container matColumnDef="valueType">
                  <th mat-header-cell *matHeaderCellDef>Type</th>
                  <td mat-cell *matCellDef="let row">{{ row.valueType }}</td>
                </ng-container>
                <ng-container matColumnDef="level">
                  <th mat-header-cell *matHeaderCellDef>Niveau</th>
                  <td mat-cell *matCellDef="let row">{{ row.level }}</td>
                </ng-container>
                <ng-container matColumnDef="exposure">
                  <th mat-header-cell *matHeaderCellDef>Exposition</th>
                  <td mat-cell *matCellDef="let row">{{ row.exposure }}</td>
                </ng-container>
                <ng-container matColumnDef="active">
                  <th mat-header-cell *matHeaderCellDef>Actif</th>
                  <td mat-cell *matCellDef="let row">
                    <span class="material-symbols-outlined" [style.color]="row.active ? 'var(--tch-color-primary)' : 'var(--tch-color-outline)'">
                      {{ row.active ? 'check_circle' : 'cancel' }}
                    </span>
                  </td>
                </ng-container>
                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef></th>
                  <td mat-cell *matCellDef="let row">
                    <div class="action-row">
                      <button mat-icon-button (click)="openEdit(row)" title="Modifier">
                        <span class="material-symbols-outlined">edit</span>
                      </button>
                      <button mat-icon-button color="warn" (click)="openDelete(row)" title="Supprimer">
                        <span class="material-symbols-outlined">delete</span>
                      </button>
                    </div>
                  </td>
                </ng-container>
                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>
          }
        </div>

        <div footer>
          <span class="footer-count">{{ totalElements() }} paramètre(s)</span>
          <div class="pagination">
            <button mat-icon-button [disabled]="page() === 0" (click)="prevPage()">
              <span class="material-symbols-outlined">chevron_left</span>
            </button>
            <span>Page {{ page() + 1 }} / {{ totalPages() }}</span>
            <button mat-icon-button [disabled]="page() + 1 >= totalPages()" (click)="nextPage()">
              <span class="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </tch-admin-crud-shell>
    </tch-admin-page-shell>
  `,
  styles: [
    `
      .kpi-row { display: flex; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
      .kpi-chip {
        display: flex; flex-direction: column; align-items: center; padding: 0.75rem 1.25rem;
        border: 1px solid var(--tch-color-outline-variant); border-radius: 0.75rem;
        background: var(--tch-color-surface-container-low);
      }
      .kpi-value { font-size: 1.5rem; font-weight: 700; color: var(--tch-color-primary); }
      .kpi-label { font-size: 0.75rem; color: var(--tch-color-on-surface-variant); }
      .table-container { overflow-x: auto; }
      table { width: 100%; }
      .action-row { display: flex; gap: 0.25rem; }
      .footer-count { font-size: 0.875rem; color: var(--tch-color-on-surface-variant); }
      .pagination { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; }
    `,
  ],
})
export class PlatformOpsSettingsPage implements OnInit {
  private readonly api = inject(PlatformSettingsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['namespace', 'settingKey', 'settingValue', 'valueType', 'level', 'exposure', 'active', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly settings = signal<SettingView[]>([]);
  readonly stats = signal<SettingsCatalogStatsView | null>(null);
  readonly search = signal('');
  readonly namespace = signal('');
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
    this.api.listSettings({ namespace: this.namespace() || undefined, q: this.search() || undefined, page: this.page(), size: 20 }).subscribe({
      next: page => {
        this.settings.set(page.content);
        this.totalElements.set(page.totalElements);
        this.totalPages.set(page.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  onSearch(v: string): void {
    this.search.set(v);
    this.page.set(0);
    this.load();
  }

  onNamespace(event: Event): void {
    this.namespace.set((event.target as HTMLInputElement).value);
    this.page.set(0);
    this.load();
  }

  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  openEdit(setting: SettingView): void {
    const ref = this.dialog.open(EditSettingDialog, { data: setting, width: '500px' });
    ref.afterClosed().subscribe((updated: SettingView | null) => {
      if (updated) {
        this.snackBar.open('Paramètre mis à jour.', 'OK', { duration: 4000 });
        this.load();
      }
    });
  }

  openDelete(setting: SettingView): void {
    const ref = this.dialog.open(DeleteSettingDialog, { data: setting, width: '400px' });
    ref.afterClosed().subscribe((confirmed: boolean) => {
      if (!confirmed) return;
      this.api.deleteSetting(setting.id.value).subscribe({
        next: () => {
          this.snackBar.open('Paramètre supprimé.', 'OK', { duration: 4000 });
          this.load();
        },
        error: (err: unknown) => {
          const pd = (err as { error?: { title?: string } })?.error;
          this.snackBar.open(pd?.title ?? 'Erreur lors de la suppression.', 'OK', { duration: 5000 });
        },
      });
    });
  }
}
