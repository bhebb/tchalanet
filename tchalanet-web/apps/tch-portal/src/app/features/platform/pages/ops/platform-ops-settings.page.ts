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
  CreateSettingRequest,
  UpdateSettingRequest,
  SettingValueType,
  SettingLevel,
  SettingExposure,
} from '../../platform-settings-api.service';

const VALUE_TYPES: SettingValueType[] = ['STRING', 'INT', 'LONG', 'DECIMAL', 'BOOLEAN', 'JSON'];
const LEVELS: SettingLevel[] = ['GLOBAL', 'TENANT', 'OUTLET', 'TERMINAL'];
const EXPOSURES: SettingExposure[] = ['INTERNAL', 'PUBLIC_RUNTIME', 'TENANT_RUNTIME', 'ADMIN_RUNTIME'];

// ── Create Setting Dialog ──────────────────────────────────────────────────────

@Component({
  selector: 'tch-create-setting-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Nouveau paramètre</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Namespace</mat-label>
          <input matInput formControlName="namespace" placeholder="ex: pos.behavior" />
          @if (form.controls.namespace.invalid && form.controls.namespace.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Clé</mat-label>
          <input matInput formControlName="settingKey" placeholder="ex: require_open_session" />
          @if (form.controls.settingKey.invalid && form.controls.settingKey.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="settingValue" rows="3"></textarea>
          @if (form.controls.settingValue.invalid && form.controls.settingValue.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <div class="row-2">
          <mat-form-field appearance="outline">
            <mat-label>Type</mat-label>
            <mat-select formControlName="valueType">
              @for (t of valueTypes; track t) { <mat-option [value]="t">{{ t }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Niveau</mat-label>
            <mat-select formControlName="level">
              @for (l of levels; track l) { <mat-option [value]="l">{{ l }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Exposition</mat-label>
          <mat-select formControlName="exposure">
            @for (e of exposures; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Tenant ID (optionnel, pour niveau TENANT)</mat-label>
          <input matInput formControlName="tenantId" />
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Créer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 480px; }
    .row-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 0.75rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class CreateSettingDialog {
  private readonly dialogRef = inject(MatDialogRef<CreateSettingDialog>);
  private readonly api = inject(PlatformSettingsApi);
  private readonly fb = inject(FormBuilder);

  readonly valueTypes = VALUE_TYPES;
  readonly levels = LEVELS;
  readonly exposures = EXPOSURES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    namespace: ['', Validators.required],
    settingKey: ['', Validators.required],
    settingValue: ['', Validators.required],
    valueType: ['STRING' as SettingValueType, Validators.required],
    level: ['GLOBAL' as SettingLevel, Validators.required],
    exposure: ['INTERNAL' as SettingExposure],
    tenantId: [''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: CreateSettingRequest = {
      namespace: v.namespace!,
      settingKey: v.settingKey!,
      settingValue: v.settingValue!,
      valueType: v.valueType!,
      level: v.level!,
      exposure: v.exposure || undefined,
      tenantId: v.tenantId || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.createSetting(req).subscribe({
      next: (created) => { this.submitting.set(false); this.dialogRef.close(created); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur lors de la création.');
      },
    });
  }
}

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
    MatSelectModule,
  ],
  template: `
    <h2 mat-dialog-title>Modifier le paramètre</h2>
    <mat-dialog-content>
      <div class="readonly-info">
        <span class="label">Namespace</span><span>{{ data.namespace }}</span>
        <span class="label">Clé</span><span class="mono">{{ data.settingKey }}</span>
        <span class="label">Type</span><span>{{ data.valueType }}</span>
        <span class="label">Niveau</span><span>{{ data.level }}</span>
      </div>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Valeur</mat-label>
          <textarea matInput formControlName="settingValue" rows="4"></textarea>
          @if (form.controls.settingValue.invalid && form.controls.settingValue.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Exposition</mat-label>
          <mat-select formControlName="exposure">
            @for (e of exposures; track e) { <mat-option [value]="e">{{ e }}</mat-option> }
          </mat-select>
        </mat-form-field>
      </form>
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="null" [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
        Enregistrer
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .readonly-info { display: grid; grid-template-columns: auto 1fr; gap: 0.25rem 0.75rem; margin-bottom: 1rem; font-size: 0.875rem; }
    .label { color: var(--tch-color-on-surface-variant); font-weight: 500; }
    .mono { font-family: monospace; }
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 440px; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class EditSettingDialog {
  protected readonly data = inject<SettingView>(MAT_DIALOG_DATA);
  private readonly dialogRef = inject(MatDialogRef<EditSettingDialog>);
  private readonly api = inject(PlatformSettingsApi);
  private readonly fb = inject(FormBuilder);

  readonly exposures = EXPOSURES;
  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    settingValue: [this.data.settingValue, Validators.required],
    exposure: [this.data.exposure as SettingExposure],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: UpdateSettingRequest = {
      settingValue: v.settingValue!,
      exposure: v.exposure || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.updateSetting(this.data.id.value, req).subscribe({
      next: (updated) => { this.submitting.set(false); this.dialogRef.close(updated); },
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
  imports: [MatButtonModule, MatDialogModule],
  template: `
    <h2 mat-dialog-title>Supprimer le paramètre</h2>
    <mat-dialog-content>
      <p>Voulez-vous vraiment supprimer <strong class="mono">{{ data.namespace }}.{{ data.settingKey }}</strong> ?</p>
      <p class="warning-text">Cette action est irréversible (soft delete).</p>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button [mat-dialog-close]="false">Annuler</button>
      <button mat-flat-button color="warn" [mat-dialog-close]="true">Supprimer</button>
    </mat-dialog-actions>
  `,
  styles: [`.mono { font-family: monospace; } .warning-text { color: var(--tch-color-error); font-size: 0.875rem; }`],
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
  template: `
    <tch-admin-page-shell title="Paramètres de la plateforme" description="Consultez, créez et modifiez les paramètres de configuration.">
      <div actions>
        <button mat-flat-button color="primary" (click)="openCreate()">
          <span class="material-symbols-outlined">add</span>
          Nouveau
        </button>
      </div>

      @if (stats()) {
        <div class="kpi-row">
          <div class="kpi-chip">
            <span class="kpi-value">{{ stats()!.totalGlobalSettings }}</span>
            <span class="kpi-label">Global</span>
          </div>
          <div class="kpi-chip">
            <span class="kpi-value">{{ stats()!.totalTenantSettings }}</span>
            <span class="kpi-label">Tenant</span>
          </div>
          <div class="kpi-chip">
            <span class="kpi-value">{{ stats()!.totalActiveSettings }}</span>
            <span class="kpi-label">Actifs</span>
          </div>
        </div>
      }

      <tch-admin-crud-shell>
        <div toolbar>
          <tch-admin-data-toolbar
            searchPlaceholder="Clé..."
            [searchValue]="settingKeyFilter()"
            (searchChange)="onKeyFilter($event)"
          >
            <mat-form-field appearance="outline" style="min-width:160px">
              <mat-label>Namespace</mat-label>
              <input matInput [value]="namespace()" (input)="onNamespace($event)" placeholder="pos.behavior" />
            </mat-form-field>
            <mat-form-field appearance="outline" style="min-width:140px">
              <mat-label>Niveau</mat-label>
              <mat-select [value]="levelFilter()" (valueChange)="onLevelFilter($event)">
                <mat-option value="">Tous</mat-option>
                @for (l of levels; track l) { <mat-option [value]="l">{{ l }}</mat-option> }
              </mat-select>
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
                  <td mat-cell *matCellDef="let row"><code>{{ row.settingKey }}</code></td>
                </ng-container>
                <ng-container matColumnDef="settingValue">
                  <th mat-header-cell *matHeaderCellDef>Valeur</th>
                  <td mat-cell *matCellDef="let row" class="value-cell">{{ row.settingValue.length > 50 ? row.settingValue.slice(0, 50) + '…' : row.settingValue }}</td>
                </ng-container>
                <ng-container matColumnDef="valueType">
                  <th mat-header-cell *matHeaderCellDef>Type</th>
                  <td mat-cell *matCellDef="let row"><span class="chip">{{ row.valueType }}</span></td>
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
                    <tch-admin-status-pill [tone]="row.active ? 'success' : 'neutral'" [label]="row.active ? 'Actif' : 'Inactif'" />
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
  styles: [`
    .kpi-row { display: flex; gap: 1rem; margin-bottom: 1rem; flex-wrap: wrap; }
    .kpi-chip { display: flex; flex-direction: column; align-items: center; padding: 0.75rem 1.25rem; border: 1px solid var(--tch-color-outline-variant); border-radius: 0.75rem; background: var(--tch-color-surface-container-low); }
    .kpi-value { font-size: 1.5rem; font-weight: 700; color: var(--tch-color-primary); }
    .kpi-label { font-size: 0.75rem; color: var(--tch-color-on-surface-variant); }
    .table-container { overflow-x: auto; }
    table { width: 100%; }
    code { font-family: monospace; font-size: 0.8125rem; }
    .chip { background: var(--tch-color-surface-container); border-radius: 0.25rem; padding: 0.125rem 0.375rem; font-size: 0.75rem; }
    .value-cell { max-width: 280px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
    .action-row { display: flex; gap: 0.25rem; }
    .footer-count { font-size: 0.875rem; color: var(--tch-color-on-surface-variant); }
    .pagination { display: flex; align-items: center; gap: 0.5rem; font-size: 0.875rem; }
  `],
})
export class PlatformOpsSettingsPage implements OnInit {
  private readonly api = inject(PlatformSettingsApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['namespace', 'settingKey', 'settingValue', 'valueType', 'level', 'exposure', 'active', 'actions'];
  readonly levels = LEVELS;

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
      level: (this.levelFilter() as import('../../platform-settings-api.service').SettingLevel) || undefined,
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
