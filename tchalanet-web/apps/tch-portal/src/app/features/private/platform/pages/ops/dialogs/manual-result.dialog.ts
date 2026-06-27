import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable, map } from 'rxjs';

import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { PlatformOpsApi, RecordManualDrawResultRequest } from '../../../platform-ops-api.service';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';

@Component({
  selector: 'tch-manual-result-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    TchSearchSelect,
  ],
  template: `
    <h2 mat-dialog-title>Saisir un résultat manuel</h2>
    <mat-dialog-content>
      <form [formGroup]="form" class="manual-result-dialog__form">
        <tch-search-select
          label="Tenant"
          placeholder="Nom ou code du tenant"
          icon="apartment"
          emptyLabel="Aucun tenant trouvé"
          [error]="form.controls.tenantId.invalid && form.controls.tenantId.touched ? 'Requis.' : ''"
          [searchFn]="searchTenants"
          (valueChange)="selectTenant($event)"
        />
        <mat-form-field appearance="outline">
          <mat-label>Slot key</mat-label>
          <input matInput formControlName="slotKey" placeholder="ny-middaynumbers" />
          @if (form.controls.slotKey.invalid && form.controls.slotKey.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Date du tirage (YYYY-MM-DD)</mat-label>
          <input matInput formControlName="drawDate" />
          @if (form.controls.drawDate.invalid && form.controls.drawDate.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 3</mat-label>
          <input matInput formControlName="pick3" placeholder="1-2-3" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Pick 4</mat-label>
          <input matInput formControlName="pick4" placeholder="1-2-3-4" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Raison</mat-label>
          <textarea matInput formControlName="reason" rows="2"></textarea>
          @if (form.controls.reason.invalid && form.controls.reason.touched) {
            <mat-error>Requis.</mat-error>
          }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="2"></textarea>
        </mat-form-field>
        <mat-checkbox formControlName="force">Forcer si le résultat existe déjà</mat-checkbox>
      </form>
      @if (error()) {
        <div class="manual-result-dialog__error">{{ error() }}</div>
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close [disabled]="submitting()">Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || submitting()" (click)="submit()">
        Saisir
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .manual-result-dialog__form { display: flex; flex-direction: column; gap: 0.75rem; width: 100%; }
    .manual-result-dialog__error { background: var(--tch-color-error-container); color: var(--tch-color-on-error-container); padding: 0.75rem; border-radius: var(--tch-radius-sm); font-size: 0.875rem; margin-top: 0.5rem; }
  `],
})
export class ManualResultDialog {
  private readonly dialogRef = inject(MatDialogRef<ManualResultDialog>);
  private readonly api = inject(PlatformOpsApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly submitting = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.group({
    tenantId: ['', Validators.required],
    slotKey: ['', Validators.required],
    drawDate: ['', Validators.required],
    pick3: [''],
    pick4: [''],
    reason: ['', Validators.required],
    notes: [''],
    force: [false],
  });

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.form.patchValue({ tenantId: tenant?.id ?? tenant?.tenantId ?? '' });
  }

  submit(): void {
    if (this.form.invalid || this.submitting()) return;
    const v = this.form.value;
    const req: RecordManualDrawResultRequest = {
      tenantId: v.tenantId!,
      slotKey: v.slotKey!,
      drawDate: v.drawDate!,
      recordedBy: 'platform-ops',
      pick3: v.pick3 || undefined,
      pick4: v.pick4 || undefined,
      reason: v.reason!,
      notes: v.notes || undefined,
      force: v.force ?? false,
    };
    this.submitting.set(true);
    this.error.set(null);
    this.api.manualDrawResult(req).subscribe({
      next: () => {
        this.submitting.set(false);
        this.snackBar.open('Résultat manuel saisi.', 'OK', { duration: 3000 });
        this.dialogRef.close(true);
      },
      error: err => {
        this.submitting.set(false);
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.');
      },
    });
  }

  private toTenantOption(tenant: TenantSummaryView): TchSearchOption<TenantSummaryView> {
    return {
      id: tenant.id ?? tenant.tenantId ?? tenant.code,
      title: tenant.name,
      subtitle: tenant.code,
      badge: tenant.status,
      icon: 'apartment',
      data: tenant,
    };
  }
}
