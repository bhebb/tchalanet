import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AdminStatusPillComponent } from '@tch/ui/console';

import { AuditEventView, auditActionTone, auditActorTone } from '../../data-access/platform-audit-api.service';

@Component({
  selector: 'tch-audit-events-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatTooltipModule, MatTableModule, AdminStatusPillComponent],
  templateUrl: './audit-events-table.component.html',
  styleUrls: ['./audit-events-table.component.scss'],
})
export class AuditEventsTableComponent {
  private readonly snackBar = inject(MatSnackBar);

  readonly events = input<readonly AuditEventView[]>([]);
  readonly showTenantColumn = input(true);

  readonly expandedId = signal<string | null>(null);

  readonly displayedColumns = computed(() => [
    'occurredAt', 'actor', 'entity', 'action',
    ...(this.showTenantColumn() ? ['tenantId'] : []),
    'ip', 'expand',
  ]);

  readonly actorTone = auditActorTone;
  readonly actionTone = auditActionTone;

  toggleExpand(id: string): void {
    this.expandedId.set(this.expandedId() === id ? null : id);
  }

  copyEntityId(entityId: string): void {
    void navigator.clipboard.writeText(entityId);
    this.snackBar.open('ID entité copié.', 'OK', { duration: 2500 });
  }

  copyActorId(actorId: string): void {
    void navigator.clipboard.writeText(actorId);
    this.snackBar.open('ID acteur copié.', 'OK', { duration: 2500 });
  }

  copyAuditId(auditId: string): void {
    void navigator.clipboard.writeText(auditId);
    this.snackBar.open('ID audit copié.', 'OK', { duration: 2500 });
  }

  detailValue(raw: string | null, key: string): string | null {
    const details = this.parseDetails(raw);
    const value = details?.[key];
    if (value === undefined || value === null || value === '') return null;
    return typeof value === 'string' ? value : JSON.stringify(value);
  }

  formatDetails(raw: string | null): string {
    if (!raw) return '';
    try { return JSON.stringify(JSON.parse(raw), null, 2); }
    catch { return raw; }
  }

  private parseDetails(raw: string | null): Record<string, unknown> | null {
    if (!raw) return null;
    try {
      const value = JSON.parse(raw);
      return value && typeof value === 'object' && !Array.isArray(value) ? value : null;
    } catch {
      return null;
    }
  }
}
