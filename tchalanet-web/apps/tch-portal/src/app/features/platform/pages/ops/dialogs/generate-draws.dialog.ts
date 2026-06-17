import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';

import { AdminStatusPillComponent } from '../../../../private/shared/admin-ui/admin-status-pill.component';
import {
  PlatformOpsApi,
  TenantBatchResponse,
  GenerateDrawsRequest,
} from '../../../platform-ops-api.service';

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
      <form [formGroup]="form" class="generate-draws-dialog__form">
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
        <div class="generate-draws-dialog__result">
          <p class="generate-draws-dialog__summary-line">
            <strong>{{ result()!.tenantsSucceeded }}</strong>/{{ result()!.tenantsRequested }} tenants OK
            @if (result()!.tenantsFailed > 0) { · <strong class="generate-draws-dialog__err">{{ result()!.tenantsFailed }} échoué(s)</strong> }
          </p>
          @if (result()!.tenants?.length) {
            <table mat-table [dataSource]="result()!.tenants" class="generate-draws-dialog__outcome-table">
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
        <div class="generate-draws-dialog__error">{{ error() }}</div>
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
    .generate-draws-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .generate-draws-dialog__result { margin-top: 1rem; }
    .generate-draws-dialog__summary-line { margin: 0 0 0.5rem; font-size: 0.9rem; }
    .generate-draws-dialog__err { color: var(--tch-color-error); }
    .generate-draws-dialog__outcome-table { width: 100%; font-size: 0.8125rem; }
    .generate-draws-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
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
