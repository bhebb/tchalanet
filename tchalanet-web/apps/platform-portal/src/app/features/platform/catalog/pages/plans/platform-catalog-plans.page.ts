import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import { PlatformCatalogApi, CatalogPlanView, CreatePlanRequest, UpdatePlanRequest } from '../../data-access/platform-catalog-api.service';

@Component({
  selector: 'tch-create-plan-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Nouveau plan</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Code</mat-label>
          <input matInput formControlName="code" placeholder="ex: STARTER" />
          @if (form.controls.code.invalid && form.controls.code.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <div style="display:flex;gap:12px">
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Prix</mat-label>
            <input matInput type="number" formControlName="priceAmount" />
          </mat-form-field>
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Devise</mat-label>
            <input matInput formControlName="currency" placeholder="HTG" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Période de facturation</mat-label>
          <mat-select formControlName="billingPeriod">
            <mat-option value="">—</mat-option>
            <mat-option value="MONTHLY">Mensuel</mat-option>
            <mat-option value="ANNUAL">Annuel</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Créer</button>
    </mat-dialog-actions>
  `,
})
export class CreatePlanDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<CreatePlanDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    priceAmount: [0],
    currency: ['HTG'],
    billingPeriod: ['MONTHLY'],
    active: [true],
  });

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreatePlanRequest = {
      code: v.code.toUpperCase(),
      name: v.name,
      description: v.description || null,
      priceAmount: v.priceAmount,
      currency: v.currency || null,
      billingPeriod: v.billingPeriod || null,
      active: v.active,
    };
    this.api.createPlan(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Edit Dialog ───────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-plan-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatCheckboxModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Modifier — {{ plan().name }}</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Nom</mat-label>
          <input matInput formControlName="name" />
          @if (form.controls.name.invalid && form.controls.name.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <div style="display:flex;gap:12px">
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Prix</mat-label>
            <input matInput type="number" formControlName="priceAmount" />
          </mat-form-field>
          <mat-form-field appearance="outline" style="flex:1">
            <mat-label>Devise</mat-label>
            <input matInput formControlName="currency" />
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Période de facturation</mat-label>
          <mat-select formControlName="billingPeriod">
            <mat-option value="">—</mat-option>
            <mat-option value="MONTHLY">Mensuel</mat-option>
            <mat-option value="ANNUAL">Annuel</mat-option>
          </mat-select>
        </mat-form-field>
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Enregistrer</button>
    </mat-dialog-actions>
  `,
})
export class EditPlanDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditPlanDialog>);
  private readonly fb = inject(FormBuilder);

  readonly saving = signal(false);
  readonly plan = signal<CatalogPlanView>({} as CatalogPlanView);
  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    priceAmount: [0 as number | null],
    currency: ['HTG'],
    billingPeriod: ['MONTHLY'],
    active: [true],
  });

  init(p: CatalogPlanView): void {
    this.plan.set(p);
    this.form.patchValue({
      name: p.name,
      description: p.description ?? '',
      priceAmount: p.priceAmount ?? 0,
      currency: p.currency ?? 'HTG',
      billingPeriod: p.billingPeriod ?? 'MONTHLY',
      active: p.active,
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpdatePlanRequest = {
      name: v.name,
      description: v.description || null,
      priceAmount: v.priceAmount,
      currency: v.currency || null,
      billingPeriod: v.billingPeriod || null,
      active: v.active,
    };
    this.api.updatePlan(this.plan().id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

@Component({
  selector: 'tch-platform-catalog-plans-page',
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
    MatIconModule,
    MatTableModule,
  ],
  templateUrl: './platform-catalog-plans.page.html',
})
export class PlatformCatalogPlansPage implements OnInit {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'name', 'priceAmount', 'billingPeriod', 'active', 'isDefault', 'actions'];
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly plans = signal<CatalogPlanView[]>([]);

  private showError(msg: string): void {
    this.error.set(msg);
    setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
  }

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.listPlans().subscribe({
      next: list => { this.plans.set(list); this.loading.set(false); },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  openCreate(): void {
    const ref = this.dialog.open(CreatePlanDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogPlanView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Plan créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(plan: CatalogPlanView): void {
    const ref = this.dialog.open(EditPlanDialog, { width: '520px' });
    (ref.componentInstance as EditPlanDialog).init(plan);
    ref.afterClosed().subscribe((updated: CatalogPlanView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Plan mis à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  deactivate(plan: CatalogPlanView): void {
    if (!confirm(`Désactiver le plan « ${plan.name} » ?`)) return;
    this.api.deactivatePlan(plan.id).subscribe({
      next: () => { this.snackBar.open('Plan désactivé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }

  delete(plan: CatalogPlanView): void {
    if (!confirm(`Supprimer le plan « ${plan.name} » ?`)) return;
    this.api.deletePlan(plan.id).subscribe({
      next: () => { this.snackBar.open('Plan supprimé.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }
}
