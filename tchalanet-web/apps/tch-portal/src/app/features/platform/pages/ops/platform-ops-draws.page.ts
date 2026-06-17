import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { AdminPageShellComponent } from '../../../private/shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent } from '../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  TenantBatchResponse,
  GenerateDrawsRequest,
  OpenTodayDrawsRequest,
  CloseDueDrawsRequest,
  ApplyExternalResultsRequest,
} from '../../platform-ops-api.service';

// ── Generate dialog (needs date range) ───────────────────────────────────────

@Component({
  selector: 'tch-generate-draws-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    AdminStatusPillComponent,
  ],
  template: `
    <h2 mat-dialog-title>Générer les tirages</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Du (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="from" />
          @if (form.controls.from.invalid && form.controls.from.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Au (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="to" />
          @if (form.controls.to.invalid && form.controls.to.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Codes tenant (optionnel, virgule-séparé)</mat-label>
          <input matInput formControlName="tenantCodes" placeholder="hbt, demo" />
        </mat-form-field>
        <mat-checkbox formControlName="dryRun">Dry-run (simuler sans créer)</mat-checkbox>
        <mat-checkbox formControlName="force">Forcer</mat-checkbox>
        @if (form.controls.force.value) {
          <mat-form-field appearance="outline">
            <mat-label>Raison</mat-label>
            <input matInput formControlName="reason" />
          </mat-form-field>
        }
      </form>
      @if (result()) {
        <div class="result-summary">
          <p class="summary-line">
            <strong>{{ result()!.tenantsSucceeded }}</strong>/{{ result()!.tenantsRequested }} tenants OK
            @if (result()!.tenantsFailed > 0) { · <strong class="err">{{ result()!.tenantsFailed }} échoué(s)</strong> }
          </p>
          @if (result()!.tenants?.length) {
            <table mat-table [dataSource]="result()!.tenants" class="outcome-table">
              <ng-container matColumnDef="tenantId">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let r"><code>{{ r.tenantId }}</code></td>
              </ng-container>
              <ng-container matColumnDef="ok">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let r">
                  <tch-admin-status-pill [tone]="r.ok ? 'success' : 'danger'" [label]="r.ok ? 'OK' : 'ERREUR'" />
                </td>
              </ng-container>
              <ng-container matColumnDef="detail">
                <th mat-header-cell *matHeaderCellDef>Détail</th>
                <td mat-cell *matCellDef="let r">
                  @if (r.ok && r.result) {
                    créés: {{ r.result.created }} · ignorés: {{ r.result.skipped }}
                  } @else if (r.error) {
                    {{ r.error }}
                  }
                </td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="outcomeColumns"></tr>
              <tr mat-row *matRowDef="let r; columns: outcomeColumns"></tr>
            </table>
          }
        </div>
      }
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Générer
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 440px; }
    .result-summary { margin-top: 1rem; }
    .summary-line { margin: 0 0 0.5rem; font-size: 0.9rem; }
    .err { color: var(--tch-color-error); }
    .outcome-table { width: 100%; font-size: 0.8125rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    code { font-family: monospace; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class GenerateDrawsDialog {
  readonly dialogRef = inject(MatDialogRef<GenerateDrawsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<TenantBatchResponse<{ created: number; skipped: number }> | null>(null);
  readonly outcomeColumns = ['tenantId', 'ok', 'detail'];

  readonly form = this.fb.group({
    from: ['', Validators.required],
    to: ['', Validators.required],
    tenantCodes: [''],
    dryRun: [true],
    force: [false],
    reason: [''],
  });

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: GenerateDrawsRequest = {
      from: v.from!,
      to: v.to!,
      tenantCodes: v.tenantCodes ? v.tenantCodes.split(',').map(s => s.trim()).filter(Boolean) : undefined,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.generateDraws(req).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res as TenantBatchResponse<{ created: number; skipped: number }>); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur.');
      },
    });
  }
}

// ── Generic batch dialog (open / close / apply) ───────────────────────────────

type AnyBatchResult = TenantBatchResponse<{ opened?: number; closed?: number; inserted?: number; applied?: number }>;

@Component({
  selector: 'tch-batch-op-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatTableModule,
    AdminStatusPillComponent,
  ],
  template: `
    <h2 mat-dialog-title>{{ data.title }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Codes tenant (optionnel, virgule-séparé)</mat-label>
          <input matInput formControlName="tenantCodes" placeholder="hbt, demo" />
        </mat-form-field>
        @if (data.hasLimit) {
          <mat-form-field appearance="outline">
            <mat-label>Limite</mat-label>
            <input matInput type="number" formControlName="limit" min="1" />
          </mat-form-field>
        }
        <mat-checkbox formControlName="dryRun">Dry-run (simuler sans appliquer)</mat-checkbox>
      </form>
      @if (result()) {
        <div class="result-summary">
          <p class="summary-line">
            <strong>{{ result()!.tenantsSucceeded }}</strong>/{{ result()!.tenantsRequested }} tenants OK
            @if (result()!.tenantsFailed > 0) { · <strong class="err">{{ result()!.tenantsFailed }} échoué(s)</strong> }
          </p>
          @if (result()!.tenants?.length) {
            <table mat-table [dataSource]="result()!.tenants" class="outcome-table">
              <ng-container matColumnDef="tenantId">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let r"><code>{{ r.tenantId }}</code></td>
              </ng-container>
              <ng-container matColumnDef="ok">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let r">
                  <tch-admin-status-pill [tone]="r.ok ? 'success' : 'danger'" [label]="r.ok ? 'OK' : 'ERREUR'" />
                </td>
              </ng-container>
              <ng-container matColumnDef="error">
                <th mat-header-cell *matHeaderCellDef>Message</th>
                <td mat-cell *matCellDef="let r">{{ r.error ?? '—' }}</td>
              </ng-container>
              <tr mat-header-row *matHeaderRowDef="outcomeColumns"></tr>
              <tr mat-row *matRowDef="let r; columns: outcomeColumns"></tr>
            </table>
          }
        </div>
      }
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Exécuter
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 420px; }
    .result-summary { margin-top: 1rem; }
    .summary-line { margin: 0 0 0.5rem; font-size: 0.9rem; }
    .err { color: var(--tch-color-error); }
    .outcome-table { width: 100%; font-size: 0.8125rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    code { font-family: monospace; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class BatchOpDialog {
  protected readonly data = inject<{
    title: string;
    hasLimit?: boolean;
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void;
  }>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<BatchOpDialog>);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<AnyBatchResult | null>(null);
  readonly outcomeColumns = ['tenantId', 'ok', 'error'];

  readonly form = this.fb.group({
    tenantCodes: [''],
    limit: [10000],
    dryRun: [true],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    const tenantCodes = v.tenantCodes ? v.tenantCodes.split(',').map(s => s.trim()).filter(Boolean) : [];
    this.submitting.set(true);
    this.data.execute(tenantCodes, v.dryRun ?? true, v.limit ?? 10000);
  }

  setResult(res: AnyBatchResult): void {
    this.submitting.set(false);
    this.result.set(res);
  }

  setError(msg: string): void {
    this.submitting.set(false);
    this.error.set(msg);
  }
}

// ── Apply dialog ──────────────────────────────────────────────────────────────

@Component({
  selector: 'tch-apply-results-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  template: `
    <h2 mat-dialog-title>Appliquer les résultats</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="dialog-form">
        <mat-form-field appearance="outline">
          <mat-label>Date de base (YYYY-MM-DD, optionnel)</mat-label>
          <input matInput formControlName="baseDate" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Jours en arrière</mat-label>
          <input matInput type="number" formControlName="daysBack" min="0" />
        </mat-form-field>
        <mat-checkbox formControlName="dryRun">Dry-run</mat-checkbox>
        <mat-checkbox formControlName="force">Forcer</mat-checkbox>
        @if (form.controls.force.value) {
          <mat-form-field appearance="outline">
            <mat-label>Raison</mat-label>
            <input matInput formControlName="reason" />
          </mat-form-field>
        }
      </form>
      @if (result()) {
        <div class="result-panel">
          Insérés: {{ result()!.inserted }} · Mis à jour: {{ result()!.updated }} · Non trouvés: {{ result()!.notFound }} · Erreurs: {{ result()!.errors }}
        </div>
      }
      @if (error()) {
        <div class="error-panel">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button (click)="dialogRef.close()">{{ result() ? 'Fermer' : 'Annuler' }}</button>
      @if (!result()) {
        <button mat-flat-button color="primary" [disabled]="submitting()" (click)="submit()">
          @if (submitting()) { <span class="spin material-symbols-outlined">progress_activity</span> }
          Appliquer
        </button>
      }
    </mat-dialog-actions>
  `,
  styles: [`
    .dialog-form { display: flex; flex-direction: column; gap: 0.75rem; min-width: 400px; }
    .result-panel { background: var(--tch-color-success-container, #d4edda); color: var(--tch-color-on-success-container, #155724); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.75rem; }
    .error-panel { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: 0.5rem; font-size: 0.875rem; margin-top: 0.5rem; }
    .spin { animation: spin 0.8s linear infinite; display: inline-block; vertical-align: middle; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `],
})
export class ApplyResultsDialog {
  readonly dialogRef = inject(MatDialogRef<ApplyResultsDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);
  readonly result = signal<{ inserted: number; updated: number; notFound: number; errors: number } | null>(null);

  readonly form = this.fb.group({
    baseDate: [''],
    daysBack: [0],
    dryRun: [true],
    force: [false],
    reason: [''],
  });

  submit(): void {
    if (this.submitting() || this.result()) return;
    const v = this.form.value;
    const req: ApplyExternalResultsRequest = {
      baseDate: v.baseDate || undefined,
      daysBack: v.daysBack ?? 0,
      dryRun: v.dryRun ?? true,
      force: v.force ?? false,
      reason: v.reason || undefined,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.applyDrawResults(req).subscribe({
      next: (res) => { this.submitting.set(false); this.result.set(res); },
      error: (err: unknown) => {
        this.submitting.set(false);
        const pd = (err as { error?: { title?: string } })?.error;
        this.error.set(pd?.title ?? 'Erreur.');
      },
    });
  }
}

// ── Main page ─────────────────────────────────────────────────────────────────

interface DrawOpCard {
  id: string;
  icon: string;
  title: string;
  description: string;
  open: () => void;
}

@Component({
  selector: 'tch-platform-ops-draws-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminPageShellComponent,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <tch-admin-page-shell
      title="Opérations — Tirages"
      description="Gestion manuelle du cycle de vie des tirages."
    >
      <div class="actions-grid">
        @for (action of drawActions; track action.id) {
          <div class="action-card">
            <div class="card-icon">
              <span class="material-symbols-outlined">{{ action.icon }}</span>
            </div>
            <div class="card-body">
              <h3 class="action-title">{{ action.title }}</h3>
              <p class="action-description">{{ action.description }}</p>
            </div>
            <button mat-flat-button color="primary" (click)="action.open()">
              <span class="material-symbols-outlined">play_arrow</span>
              Exécuter
            </button>
          </div>
        }
      </div>
    </tch-admin-page-shell>
  `,
  styles: [`
    .actions-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 1.25rem; margin-top: 1rem; }
    .action-card { border: 1px solid var(--tch-color-outline-variant); border-radius: 0.75rem; padding: 1.25rem; display: flex; flex-direction: column; gap: 0.75rem; }
    .card-icon { color: var(--tch-color-primary); }
    .card-icon .material-symbols-outlined { font-size: 1.75rem; }
    .action-title { margin: 0; font-size: 1rem; font-weight: 600; }
    .action-description { margin: 0; font-size: 0.875rem; color: var(--tch-color-on-surface-variant); flex: 1; }
  `],
})
export class PlatformOpsDrawsPage {
  private readonly api = inject(PlatformOpsApi);
  private readonly dialog = inject(MatDialog);

  readonly drawActions: DrawOpCard[] = [
    {
      id: 'generate',
      icon: 'event_note',
      title: 'Générer les tirages',
      description: 'Crée les tirages pour une plage de dates. Idempotent — ignore ceux déjà existants.',
      open: () => this.dialog.open(GenerateDrawsDialog, { width: '520px' }),
    },
    {
      id: 'open-today',
      icon: 'lock_open',
      title: "Ouvrir les tirages du jour",
      description: "Ouvre tous les tirages dont la date est aujourd'hui (miroir du scheduler).",
      open: () => this.openBatch("Ouvrir les tirages du jour", true, (tenantCodes, dryRun, limit) => {
        const req: OpenTodayDrawsRequest = { tenantCodes, limit, dryRun };
        this.api.openTodayDraws(req).subscribe({
          next: (res) => this.currentBatchDialog?.setResult(res as AnyBatchResult),
          error: (err: unknown) => {
            const pd = (err as { error?: { title?: string } })?.error;
            this.currentBatchDialog?.setError(pd?.title ?? 'Erreur.');
          },
        });
      }),
    },
    {
      id: 'close-due',
      icon: 'lock',
      title: 'Fermer les tirages échus',
      description: "Ferme tous les tirages dont l'heure de clôture est dépassée.",
      open: () => this.openBatch("Fermer les tirages échus", true, (tenantCodes, dryRun, limit) => {
        const req: CloseDueDrawsRequest = { tenantCodes, limit, dryRun };
        this.api.closeDueDraws(req).subscribe({
          next: (res) => this.currentBatchDialog?.setResult(res as AnyBatchResult),
          error: (err: unknown) => {
            const pd = (err as { error?: { title?: string } })?.error;
            this.currentBatchDialog?.setError(pd?.title ?? 'Erreur.');
          },
        });
      }),
    },
    {
      id: 'apply',
      icon: 'assignment_turned_in',
      title: 'Appliquer les résultats',
      description: 'Applique les résultats confirmés aux tirages et déclenche le règlement.',
      open: () => this.dialog.open(ApplyResultsDialog, { width: '460px' }),
    },
  ];

  private currentBatchDialog: BatchOpDialog | null = null;

  private openBatch(
    title: string,
    hasLimit: boolean,
    execute: (tenantCodes: string[], dryRun: boolean, limit?: number) => void,
  ): void {
    const ref = this.dialog.open(BatchOpDialog, {
      data: { title, hasLimit, execute },
      width: '500px',
    });
    this.currentBatchDialog = ref.componentInstance;
    ref.afterClosed().subscribe(() => { this.currentBatchDialog = null; });
  }
}
