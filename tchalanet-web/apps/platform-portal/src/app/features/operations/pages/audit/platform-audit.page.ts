import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Observable, map } from 'rxjs';

import {
  AdminListStatusOption,
  AdminListSurface,
  TchErrorPanel,
  TchLoading,
  TchSearchOption,
  TchSearchSelect,
} from '@tch/ui/components';
import { AdminEmptyStateComponent } from '@tch/ui/console';
import { AdminPageShellComponent } from '@tch/ui/console';
import { PlatformTenantsApi, TenantSummaryView } from '../../../tenants/data-access/platform-tenants-api.service';
import { AuditEntityType, AuditEventView, PlatformAuditApi } from '../../data-access/platform-audit-api.service';
import { AuditEventsTableComponent } from '../../components/audit-events-table/audit-events-table.component';

export const AUDIT_ENTITY_TYPES: AuditEntityType[] = [
  'SYSTEM', 'TENANT', 'PLAN', 'SUBSCRIPTION', 'THEME', 'USER', 'USER_PREFERENCE',
  'GAME', 'DRAW', 'DRAW_RESULT', 'DRAW_CHANNEL', 'RESULT_SLOT', 'ODDS', 'LIMIT_POLICY',
  'OUTLET', 'TERMINAL', 'SELLER_TERMINAL', 'TICKET', 'TICKET_LINE', 'PAYOUT', 'PAYMENT',
  'FEATURE_FLAG', 'BATCH_JOB', 'CACHE', 'PUBLIC_CONTENT',
];

@Component({
  selector: 'tch-platform-audit-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminListSurface,
    AuditEventsTableComponent,
    TchErrorPanel,
    TchLoading,
    TchSearchSelect,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './platform-audit.page.html',
  styleUrls: ['./platform-audit.page.scss'],
})
export class PlatformAuditPage implements OnInit {
  private readonly api = inject(PlatformAuditApi);
  private readonly tenantsApi = inject(PlatformTenantsApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly entityTypes = AUDIT_ENTITY_TYPES;

  readonly filterForm = this.fb.nonNullable.group({
    entityType: [''],
    action: [''],
    actorId: [''],
    entityId: [''],
    tenantId: [''],
    ip: [''],
    from: [''],
    to: [''],
  });

  readonly loading = signal(false);
  readonly purging = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<AuditEventView[]>([]);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly hasNext = signal(false);
  readonly hasPrevious = signal(false);
  readonly hasActiveFilters = signal(false);

  readonly searchTenants = (query: string): Observable<readonly TchSearchOption<TenantSummaryView>[]> =>
    this.tenantsApi.listTenants({ q: query, page: 0, size: 12, status: null }).pipe(
      map(page => page.items.map(tenant => this.toTenantOption(tenant))),
    );

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    const v = this.filterForm.getRawValue();
    this.api.listAuditEvents({
      entityType: (v.entityType as AuditEntityType) || undefined,
      action: v.action || undefined,
      actorId: v.actorId || undefined,
      entityId: v.entityId || undefined,
      tenantId: v.tenantId || undefined,
      ip: v.ip || undefined,
      from: v.from ? new Date(v.from).toISOString() : undefined,
      to: v.to ? new Date(v.to + 'T23:59:59').toISOString() : undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.events.set(p.items);
        this.totalElements.set(p.totalElements);
        this.page.set(p.page);
        this.totalPages.set(p.totalPages || 1);
        this.hasNext.set(p.hasNext ?? false);
        this.hasPrevious.set(p.hasPrevious ?? false);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void {
    this.page.set(0);
    this.updateActiveFilters();
    this.load();
  }

  resetFilters(): void {
    this.filterForm.reset();
    this.page.set(0);
    this.hasActiveFilters.set(false);
    this.load();
  }

  entityTypeOptions(): readonly AdminListStatusOption[] {
    return this.entityTypes.map(entityType => ({ value: entityType, label: entityType }));
  }

  onEntityIdFilter(entityId: string): void {
    this.filterForm.patchValue({ entityId }, { emitEvent: false });
    this.applyFilters();
  }

  onEntityTypeFilter(entityType: string): void {
    this.filterForm.patchValue({ entityType }, { emitEvent: false });
    this.applyFilters();
  }

  selectTenantFilter(option: TchSearchOption | null): void {
    const tenant = option?.data as TenantSummaryView | undefined;
    this.filterForm.patchValue({
      tenantId: tenant?.id ?? tenant?.tenantId ?? '',
    });
  }

  prevPage(): void { if (this.hasPrevious()) { this.page.set(this.page() - 1); this.load(); } }
  nextPage(): void { if (this.hasNext()) { this.page.set(this.page() + 1); this.load(); } }

  purge(): void {
    const reason = prompt('Raison obligatoire pour la purge de rétention audit:');
    if (!reason?.trim()) return;
    const confirmation = prompt(
      'Action sensible: tapez PURGER pour supprimer uniquement les logs expirés selon la politique de rétention.',
    );
    if (confirmation !== 'PURGER') return;
    this.purging.set(true);
    this.api.purge(reason.trim()).subscribe({
      next: result => {
        this.purging.set(false);
        this.snackBar.open(`${result.deleted} événement(s) purgé(s) (rétention ${result.retentionDays}j).`, 'OK', { duration: 6000 });
        this.load();
      },
      error: (err: unknown) => {
        this.purging.set(false);
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? 'Erreur lors de la purge.',
          'OK', { duration: 5000 },
        );
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

  private updateActiveFilters(): void {
    const v = this.filterForm.getRawValue();
    this.hasActiveFilters.set(Object.values(v).some(value => !!value));
  }
}
