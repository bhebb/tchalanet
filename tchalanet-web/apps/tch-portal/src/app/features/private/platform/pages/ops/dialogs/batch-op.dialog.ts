import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { AdminStatusPillComponent } from '../../../../shared/admin-ui/admin-status-pill.component';
import { OpsLaunchResponse } from '../../../platform-ops-api.service';

export type AnyBatchResult = OpsLaunchResponse;

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
      <form [formGroup]="form" class="batch-op-dialog__form">
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
        <div class="batch-op-dialog__result">
          <p class="batch-op-dialog__summary-line">
            <strong>{{ result()!.started }}</strong>/{{ result()!.requested }} job(s) lancé(s)
            @if (result()!.failed > 0) { · <strong class="batch-op-dialog__err">{{ result()!.failed }} échoué(s)</strong> }
          </p>
          @if (result()!.launches.length) {
            <table mat-table [dataSource]="result()!.launches" class="batch-op-dialog__outcome-table">
              <ng-container matColumnDef="tenantId">
                <th mat-header-cell *matHeaderCellDef>Tenant</th>
                <td mat-cell *matCellDef="let r"><code>{{ r.tenant_id ?? 'global' }}</code></td>
              </ng-container>
              <ng-container matColumnDef="ok">
                <th mat-header-cell *matHeaderCellDef>Statut</th>
                <td mat-cell *matCellDef="let r">
                  <tch-admin-status-pill [tone]="r.error ? 'danger' : 'success'" [label]="r.status" />
                </td>
              </ng-container>
              <ng-container matColumnDef="error">
                <th mat-header-cell *matHeaderCellDef>Détail</th>
                <td mat-cell *matCellDef="let r">
                  @if (!r.error && r.execution_id) {
                    execution #{{ r.execution_id }}
                  } @else {
                    {{ r.error ?? '—' }}
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
        <div class="batch-op-dialog__error">{{ error() }}</div>
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
    .batch-op-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .batch-op-dialog__result { margin-top: 1rem; }
    .batch-op-dialog__summary-line { margin: 0 0 0.5rem; font-size: 0.9rem; }
    .batch-op-dialog__err { color: var(--tch-color-error); }
    .batch-op-dialog__outcome-table { width: 100%; font-size: 0.8125rem; }
    .batch-op-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
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
