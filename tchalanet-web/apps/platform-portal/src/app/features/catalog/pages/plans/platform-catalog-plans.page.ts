import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
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

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { AdminStatusPillComponent } from '@tch/ui/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { PlatformCatalogApi, CatalogPlanView, UpdatePlanRequest } from '../../data-access/platform-catalog-api.service';
import { CreatePlanDialog } from '../../components/dialogs/create-plan.dialog';

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
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-plans.page.html',
  styleUrls: ['./platform-catalog-plans.page.scss'],
})
export class PlatformCatalogPlansPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly displayedColumns = ['code', 'name', 'priceAmount', 'billingPeriod', 'active', 'isDefault', 'actions'];
  readonly plans = this.api.listPlansResource({ suppressShellFeedback: true });
  readonly plansError = resourceErrorVm(this.plans, 'platform.catalog.plans');
  readonly planRows = computed(() => this.plans.value() ?? []);
  readonly selectedPlan = signal<CatalogPlanView | null>(null);
  readonly savingConfig = signal(false);

  readonly configForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    priceAmount: [0 as number | null],
    currency: ['HTG'],
    billingPeriod: ['MONTHLY'],
  });

  private showError(msg: string): void {
    this.snackBar.open(msg, 'OK', { duration: 5000 });
  }

  load(): void {
    this.plans.reload();
  }

  openCreate(): void {
    const ref = this.dialog.open(CreatePlanDialog, { width: '520px' });
    ref.afterClosed().subscribe((created: CatalogPlanView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Plan créé.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openConfig(plan: CatalogPlanView): void {
    this.selectedPlan.set(plan);
    this.configForm.reset({
      name: plan.name,
      description: plan.description ?? '',
      priceAmount: plan.priceAmount ?? 0,
      currency: plan.currency ?? 'HTG',
      billingPeriod: plan.billingPeriod ?? 'MONTHLY',
    });
  }

  closeConfig(): void {
    this.selectedPlan.set(null);
  }

  saveConfig(): void {
    const plan = this.selectedPlan();
    if (!plan || this.configForm.invalid || this.savingConfig()) return;

    this.savingConfig.set(true);
    const v = this.configForm.getRawValue();
    const req: UpdatePlanRequest = {
      name: v.name,
      description: v.description || null,
      priceAmount: v.priceAmount,
      currency: v.currency || null,
      billingPeriod: v.billingPeriod || null,
    };
    this.api.updatePlan(plan.id, req).subscribe({
      next: updated => {
        this.savingConfig.set(false);
        this.snackBar.open('Plan mis à jour.', 'OK', { duration: 4000 });
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

  formatJson(value: unknown): string {
    if (value === null || value === undefined) return 'null';
    return JSON.stringify(value, null, 2);
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
