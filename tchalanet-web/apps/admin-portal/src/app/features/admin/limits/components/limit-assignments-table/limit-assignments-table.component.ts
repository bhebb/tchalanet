import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';

import { AdminStatusPillComponent } from '@tch/ui/console';
import type { AdminStatusTone } from '@tch/ui/console';

import type { BreachOutcome, RuleRow } from '../../data-access/admin-limits.models';

@Component({
  selector: 'tch-limit-assignments-table',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    SlicePipe,
    MatButtonModule,
    MatExpansionModule,
    MatIconModule,
    MatTableModule,
    AdminStatusPillComponent,
  ],
  templateUrl: './limit-assignments-table.component.html',
  styleUrl: './limit-assignments-table.component.scss',
})
export class LimitAssignmentsTableComponent {
  readonly rows = input.required<RuleRow[]>();
  readonly canEdit = input<boolean>(false);
  readonly upsert = output<RuleRow>();
  readonly delete = output<RuleRow>();

  readonly displayedColumns = ['rule', 'info', 'status', 'onBreach', 'actions'];

  readonly categories = computed(() => {
    const seen = new Set<string>();
    const result: string[] = [];
    for (const r of this.rows()) {
      if (!seen.has(r.spec.category)) {
        seen.add(r.spec.category);
        result.push(r.spec.category);
      }
    }
    return result;
  });

  readonly rowsByCategory = computed(() => {
    const map = new Map<string, RuleRow[]>();
    for (const row of this.rows()) {
      if (!map.has(row.spec.category)) map.set(row.spec.category, []);
      const bucket = map.get(row.spec.category);
      if (bucket) bucket.push(row);
    }
    return map;
  });

  categoryLabel(category: string): string {
    return category
      .split('_')
      .map(p => p.charAt(0).toUpperCase() + p.slice(1).toLowerCase())
      .join(' ');
  }

  assignmentTone(row: RuleRow): AdminStatusTone {
    if (!row.assignment) return 'neutral';
    return row.assignment.enabled ? 'success' : 'warning';
  }

  assignmentLabel(row: RuleRow): string {
    if (!row.assignment) return 'Non configurée';
    return row.assignment.enabled ? 'Active' : 'Désactivée';
  }

  breachTone(outcome: BreachOutcome | undefined): AdminStatusTone {
    if (outcome === 'BLOCK') return 'danger';
    if (outcome === 'WARN') return 'info';
    if (outcome === 'REQUIRE_APPROVAL') return 'warning';
    return 'neutral';
  }

  paramsPreview(row: RuleRow): string {
    if (!row.assignment?.params) return '';
    try {
      const p = row.assignment.params as Record<string, unknown>;
      return Object.entries(p)
        .map(([k, v]) => `${k}: ${v}`)
        .join(', ');
    } catch {
      return '';
    }
  }
}
