import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialog, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { TchSearchOption, TchSearchSelect } from '@tch/ui/components';
import { Observable, map } from 'rxjs';

import { AdminCrudShellComponent } from '@tch/ui/console';
import { AdminDataToolbarComponent } from '@tch/ui/console';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import {
  ConsolePricingActionEvent,
  ConsolePricingRow,
  ConsolePricingTableComponent,
} from '@tch/web/console';
import {
  TchAsyncReadyDirective,
  TchAsyncViewComponent,
  resourceErrorVm,
} from '@tch/web/async';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';
import {
  BetType,
  CatalogPricingView,
  CreatePricingRequest,
  PlatformCatalogApi,
  UpdatePricingRequest,
} from '../../data-access/platform-catalog-api.service';

export const BET_TYPES: BetType[] = [
  'MATCH_1_2D', 'MATCH_2_2D', 'MATCH_3_2D',
  'LOTTO3_3D', 'MARRIAGE_2D2D',
  'LOTTO4_PATTERN', 'LOTTO5_PATTERN',
];

// ── Create Dialog ─────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-create-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule, TchSearchSelect],
  template: `
    <h2 mat-dialog-title>Nouvelle cote</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <mat-form-field appearance="outline">
          <mat-label>Jeu (gameCode)</mat-label>
          <input matInput formControlName="gameCode" placeholder="ex: BORLETTE" />
          @if (form.controls.gameCode.invalid && form.controls.gameCode.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Type de pari</mat-label>
          <mat-select formControlName="betType">
            @for (bt of betTypes; track bt) { <mat-option [value]="bt">{{ bt }}</mat-option> }
          </mat-select>
          @if (form.controls.betType.invalid && form.controls.betType.touched) { <mat-error>Requis.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Option de pari (betOption)</mat-label>
          <input matInput type="number" formControlName="betOption" placeholder="ex: 1" />
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Cote (odds)</mat-label>
          <input matInput type="number" formControlName="odds" placeholder="ex: 55" />
          @if (form.controls.odds.invalid && form.controls.odds.touched) { <mat-error>Requis, > 0.</mat-error> }
        </mat-form-field>
        <tch-search-select
          label="Tenant"
          placeholder="Laisser vide = global"
          icon="apartment"
          clearLabel="Global"
          emptyLabel="Aucun tenant trouvé"
          [searchFn]="searchTenants"
          (valueChange)="selectTenant($event)"
        />
        <mat-checkbox formControlName="active">Actif</mat-checkbox>
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-stroked-button mat-dialog-close>Annuler</button>
      <button mat-flat-button color="primary" [disabled]="form.invalid || saving()" (click)="save()">Créer</button>
    </mat-dialog-actions>
  `,
})
export class CreatePricingDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly ref = inject(MatDialogRef<CreatePricingDialog>);
  private readonly fb = inject(FormBuilder);

  readonly betTypes = BET_TYPES;
  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    gameCode: ['', Validators.required],
    betType: ['' as BetType, Validators.required],
    betOption: [null as number | null],
    odds: [null as number | null, [Validators.required, Validators.min(0.01)]],
    tenantId: [''],
    active: [true],
  });

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  selectTenant(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.form.patchValue({
      tenantId: tenant?.id ?? tenant?.tenantId ?? '',
    });
  }

  save(): void {
    if (this.form.invalid) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: CreatePricingRequest = {
      gameCode: v.gameCode.toUpperCase(),
      betType: v.betType,
      betOption: v.betOption,
      odds: v.odds!,
      tenantId: v.tenantId || null,
      active: v.active,
    };
    this.api.createPricing(req).subscribe({
      next: created => this.ref.close(created),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
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

// ── Edit Dialog ───────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-edit-pricing-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatCheckboxModule],
  template: `
    <h2 mat-dialog-title>Modifier la cote</h2>
    <mat-dialog-content>
      <form [formGroup]="form" style="display:flex;flex-direction:column;gap:12px;padding-top:8px">
        <div style="font-size:0.85rem;color:var(--tch-text-secondary,#666);padding:4px 0">
          <code>{{ row()?.gameCode }} / {{ row()?.betType }}</code>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Cote (odds)</mat-label>
          <input matInput type="number" formControlName="odds" />
          @if (form.controls.odds.invalid && form.controls.odds.touched) { <mat-error>Requis, > 0.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Option de pari (betOption)</mat-label>
          <input matInput type="number" formControlName="betOption" />
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
export class EditPricingDialog {
  private readonly api = inject(PlatformCatalogApi);
  private readonly ref = inject(MatDialogRef<EditPricingDialog>);
  private readonly fb = inject(FormBuilder);

  readonly row = signal<CatalogPricingView | null>(null);
  readonly saving = signal(false);
  readonly form = this.fb.nonNullable.group({
    odds: [null as number | null, [Validators.required, Validators.min(0.01)]],
    betOption: [null as number | null],
    active: [true],
  });

  init(r: CatalogPricingView): void {
    this.row.set(r);
    this.form.patchValue({ odds: r.odds, betOption: r.betOption, active: r.active });
  }

  save(): void {
    if (this.form.invalid || !this.row()) return;
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req: UpdatePricingRequest = { odds: v.odds, betOption: v.betOption, active: v.active };
    this.api.updatePricing(this.row()!.id, req).subscribe({
      next: updated => this.ref.close(updated),
      error: (err: unknown) => { this.ref.close({ __error: (err as { error?: { title?: string } })?.error?.title ?? 'Erreur.' }); },
    });
  }
}

// ── Main Page ─────────────────────────────────────────────────────────────────
@Component({
  selector: 'tch-platform-catalog-pricing-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminCrudShellComponent,
    AdminDataToolbarComponent,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    ConsolePricingTableComponent,
    TchAsyncReadyDirective,
    TchAsyncViewComponent,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    ReactiveFormsModule,
  ],
  templateUrl: './platform-catalog-pricing.page.html',
})
export class PlatformCatalogPricingPage {
  private readonly api = inject(PlatformCatalogApi);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly betTypes = BET_TYPES;

  readonly pricing = this.api.listPricingResource({ suppressShellFeedback: true });
  readonly pricingError = resourceErrorVm(this.pricing, 'platform.catalog.pricing');
  readonly allRows = computed(() => this.pricing.value() ?? []);
  readonly gameFilter = signal('');
  readonly betTypeFilter = signal<BetType | ''>('');

  readonly rows = computed(() => {
    let r = this.allRows();
    const gf = this.gameFilter().toUpperCase();
    if (gf) r = r.filter(x => x.gameCode.toUpperCase().includes(gf));
    const btf = this.betTypeFilter();
    if (btf) r = r.filter(x => x.betType === btf);
    return r;
  });
  readonly pricingRows = computed<readonly ConsolePricingRow[]>(() =>
    this.rows().map(row => ({
      id: row.id,
      gameCode: row.gameCode,
      betType: row.betType,
      betOption: row.betOption,
      odds: row.odds,
      tenantLabel: this.tenantLabel(row),
      statusLabel: row.active ? 'Actif' : 'Inactif',
      statusTone: row.active ? 'success' : 'neutral',
      actions: [
        { id: 'edit', label: 'Modifier', icon: 'edit', variant: 'icon' },
        { id: 'delete', label: 'Supprimer', icon: 'delete', tone: 'danger', variant: 'icon' },
      ],
    })),
  );

  private showError(msg: string): void {
    this.snackBar.open(msg, 'OK', { duration: 5000 });
  }

  load(): void {
    this.pricing.reload();
  }

  onGameFilter(v: string): void { this.gameFilter.set(v); }
  onBetTypeFilter(v: BetType | ''): void { this.betTypeFilter.set(v); }

  openCreate(): void {
    const ref = this.dialog.open(CreatePricingDialog, { width: '500px' });
    ref.afterClosed().subscribe((created: CatalogPricingView | { __error: string } | null) => {
      if (created && '__error' in created) { this.showError(created.__error); return; }
      if (created) { this.snackBar.open('Cote créée.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  openEdit(row: CatalogPricingView): void {
    const ref = this.dialog.open(EditPricingDialog, { width: '440px' });
    (ref.componentInstance as EditPricingDialog).init(row);
    ref.afterClosed().subscribe((updated: CatalogPricingView | { __error: string } | null) => {
      if (updated && '__error' in updated) { this.showError(updated.__error); return; }
      if (updated) { this.snackBar.open('Cote mise à jour.', 'OK', { duration: 4000 }); this.load(); }
    });
  }

  delete(row: CatalogPricingView): void {
    if (!confirm(`Supprimer la cote ${row.gameCode} / ${row.betType} ?`)) return;
    this.api.deletePricing(row.id).subscribe({
      next: () => { this.snackBar.open('Cote supprimée.', 'OK', { duration: 4000 }); this.load(); },
      error: (err: unknown) => {
        this.snackBar.open((err as { error?: { title?: string } })?.error?.title ?? 'Erreur.', 'OK', { duration: 5000 });
      },
    });
  }

  onPricingAction(event: ConsolePricingActionEvent): void {
    const row = this.rows().find(item => item.id === event.row.id);
    if (!row) return;
    switch (event.action.id) {
      case 'edit':
        this.openEdit(row);
        break;
      case 'delete':
        this.delete(row);
        break;
    }
  }

  tenantLabel(row: CatalogPricingView): string {
    if (!row.tenantId) return 'Global';
    return row.tenantId.slice(0, 8) + '…';
  }
}
