import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminCrudShellComponent } from '../../../shared/admin-ui/admin-crud-shell.component';
import { AdminEmptyStateComponent } from '../../../shared/admin-ui/admin-empty-state.component';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminStatusPillComponent, AdminStatusTone } from '../../../shared/admin-ui/admin-status-pill.component';
import { AuditEntityType, AuditEventView, PlatformAuditApi } from '../../platform-audit-api.service';

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
    DatePipe,
    ReactiveFormsModule,
    AdminCrudShellComponent,
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
    MatTooltipModule,
  ],
  templateUrl: './platform-audit.page.html',
  styleUrls: ['./platform-audit.page.scss'],
})
export class PlatformAuditPage implements OnInit {
  private readonly api = inject(PlatformAuditApi);
  private readonly snackBar = inject(MatSnackBar);
  private readonly fb = inject(FormBuilder);

  readonly entityTypes = AUDIT_ENTITY_TYPES;
  readonly displayedColumns = ['occurredAt', 'actor', 'entity', 'action', 'tenantId', 'ip', 'expand'];

  readonly filterForm = this.fb.nonNullable.group({
    entityType: [''],
    action: [''],
    actorId: [''],
    entityId: [''],
    tenantId: [''],
    from: [''],
    to: [''],
  });

  readonly loading = signal(false);
  readonly purging = signal(false);
  readonly error = signal<string | null>(null);
  readonly events = signal<AuditEventView[]>([]);
  readonly expandedId = signal<string | null>(null);
  readonly page = signal(0);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

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
      from: v.from ? new Date(v.from).toISOString() : undefined,
      to: v.to ? new Date(v.to + 'T23:59:59').toISOString() : undefined,
      page: this.page(),
      size: 20,
    }).subscribe({
      next: p => {
        this.events.set(p.items);
        this.totalElements.set(p.totalElements);
        this.totalPages.set(p.totalPages || 1);
        this.loading.set(false);
      },
      error: (err: unknown) => {
        this.error.set((err as { error?: { title?: string } })?.error?.title ?? 'Erreur de chargement.');
        this.loading.set(false);
      },
    });
  }

  applyFilters(): void { this.page.set(0); this.load(); }

  resetFilters(): void { this.filterForm.reset(); this.page.set(0); this.load(); }

  prevPage(): void { this.page.set(this.page() - 1); this.load(); }
  nextPage(): void { this.page.set(this.page() + 1); this.load(); }

  toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  formatDetails(raw: string | null): string {
    if (!raw) return '';
    try { return JSON.stringify(JSON.parse(raw), null, 2); }
    catch { return raw; }
  }

  purge(): void {
    if (!confirm('Purger les logs d\'audit expirés ?')) return;
    this.purging.set(true);
    this.api.purge().subscribe({
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

  actorTone(actorType: string): AdminStatusTone {
    if (actorType === 'SYSTEM') return 'neutral';
    if (actorType === 'TERMINAL') return 'warning';
    return 'info';
  }

  actionTone(action: string): AdminStatusTone {
    if (/DELETE|PURGE|VOID|CANCEL|DISABLE|BLOCK|LOCK|REVOKE/.test(action)) return 'danger';
    if (/CREATE|GENERATE|OPEN|REGISTER|ACTIVATE|RESTORE/.test(action)) return 'success';
    if (/UPDATE|STATE_CHANGE|OVERRIDE|CORRECT|SETTLE/.test(action)) return 'warning';
    return 'neutral';
  }
}
