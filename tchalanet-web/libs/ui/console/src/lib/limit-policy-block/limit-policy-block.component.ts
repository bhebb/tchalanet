import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  signal,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export type LimitGroup = 'VENTE' | 'RESTRICTIONS' | 'EXPOSITION';

export interface LimitBlockSpec {
  ruleKey: string;
  label: string;
  description?: string | null;
  stateless: boolean;
  paramsTemplate: unknown;
}

export interface LimitBlockAssignment {
  id: { value: string };
  ruleKey: string;
  enabled: boolean;
  onBreach: string;
  params: unknown;
}

export interface LimitPolicyEditRequest {
  spec: LimitBlockSpec;
  assignment: LimitBlockAssignment | null;
  inheritedAssignment: LimitBlockAssignment | null;
}

interface GroupRow {
  spec: LimitBlockSpec;
  assignment: LimitBlockAssignment | null;
  inheritedAssignment: LimitBlockAssignment | null;
  displayValue: string | null;
  inheritedDisplayValue: string | null;
}

interface GroupSection {
  id: LimitGroup;
  label: string;
  rows: GroupRow[];
}

const GROUP_RULES: Record<LimitGroup, string[]> = {
  VENTE: ['MAX_STAKE_PER_LINE', 'MAX_LINES_PER_TICKET', 'MAX_STAKE_PER_TICKET'],
  RESTRICTIONS: ['BLOCK_BET_TYPE', 'BLOCK_SELECTION_PER_DRAW'],
  EXPOSITION: [
    'MAX_STAKE_EXPOSURE_PER_SELECTION_PER_DRAW',
    'MAX_SALES_COUNT_PER_SELECTION_PER_DRAW',
  ],
};

const GROUP_LABELS: Record<LimitGroup, string> = {
  VENTE: 'Vente',
  RESTRICTIONS: 'Restrictions',
  EXPOSITION: 'Exposition',
};

const GROUPS: LimitGroup[] = ['VENTE', 'RESTRICTIONS', 'EXPOSITION'];

function formatAssignmentParams(assignment: LimitBlockAssignment): string {
  const p = assignment.params as Record<string, unknown> | null;
  if (!p) return 'Configuré';
  const centsKey = Object.keys(p).find(k => k.toLowerCase().includes('cents'));
  if (centsKey) {
    const val = p[centsKey];
    if (typeof val === 'number') {
      return `${new Intl.NumberFormat('fr-HT', { maximumFractionDigits: 0 }).format(val / 100)} G`;
    }
  }
  const countKey = Object.keys(p).find(
    k => k.toLowerCase().includes('count') || k.toLowerCase().includes('max'),
  );
  if (countKey) {
    const val = p[countKey];
    if (typeof val === 'number') return `${val}`;
  }
  const selKey = Object.keys(p).find(k => k.toLowerCase().includes('selection'));
  if (selKey) {
    const val = p[selKey];
    if (Array.isArray(val) && val.length > 0) return val.join(', ');
  }
  return 'Configuré';
}

@Component({
  selector: 'tch-limit-policy-block',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  template: `
    <div class="lpb">
      @for (group of sections(); track group.id) {
        <div class="lpb__group">
          <button
            type="button"
            class="lpb__group-header"
            [attr.aria-expanded]="isExpanded(group.id)"
            (click)="toggleGroup(group.id)"
          >
            <span class="lpb__group-label">{{ group.label }}</span>
            <span class="lpb__group-count">
              {{ activeCount(group) }}/{{ group.rows.length }}
            </span>
            <mat-icon class="lpb__group-chevron" aria-hidden="true">
              {{ isExpanded(group.id) ? 'expand_less' : 'expand_more' }}
            </mat-icon>
          </button>

          @if (isExpanded(group.id)) {
            <div class="lpb__group-body">
              @for (row of group.rows; track row.spec.ruleKey) {
                <div class="lpb__row" [class.lpb__row--active]="row.assignment !== null">
                  <div class="lpb__row-info">
                    <span class="lpb__row-label">{{ row.spec.label || row.spec.ruleKey }}</span>
                    @if (row.assignment !== null) {
                      <span class="lpb__row-value">{{ row.displayValue }}</span>
                    } @else if (row.inheritedAssignment !== null) {
                      <span class="lpb__row-inherited">
                        Hérite du {{ inheritedScopeLabel() }} · {{ row.inheritedDisplayValue }}
                      </span>
                    } @else {
                      <span class="lpb__row-empty">Non configuré</span>
                    }
                  </div>
                  <div class="lpb__row-actions">
                    <button
                      type="button"
                      mat-icon-button
                      class="lpb__action-btn"
                      [attr.aria-label]="'Modifier ' + row.spec.label"
                      (click)="onEdit(row)"
                    >
                      <mat-icon>edit</mat-icon>
                    </button>
                    @if (row.assignment !== null) {
                      <button
                        type="button"
                        mat-icon-button
                        class="lpb__action-btn lpb__action-btn--delete"
                        [attr.aria-label]="'Supprimer ' + row.spec.label"
                        (click)="onDelete(row)"
                      >
                        <mat-icon>delete</mat-icon>
                      </button>
                    }
                  </div>
                </div>
              }
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [
    `
      @use 'breakpoints' as ui;

      :host {
        display: block;
      }

      .lpb {
        display: flex;
        flex-direction: column;
        gap: 0;
      }

      /* ─── Group ─────────────────────────────────────────────────────────── */

      .lpb__group {
        border: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        border-radius: var(--tch-radius-lg, 0.75rem);
        overflow: hidden;
        margin-bottom: 0.5rem;
      }

      .lpb__group-header {
        width: 100%;
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.75rem 1rem;
        background: var(--tch-color-surface-container-low, #f4f3f7);
        border: none;
        cursor: pointer;
        text-align: start;
        transition: background 150ms ease;
      }

      .lpb__group-header:hover {
        background: var(--tch-color-surface-container, #eeedf1);
      }

      .lpb__group-label {
        flex: 1;
        font-size: 0.875rem;
        font-weight: 600;
        color: var(--tch-color-on-surface, #1a1c1e);
      }

      .lpb__group-count {
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-variant-numeric: tabular-nums;
      }

      .lpb__group-chevron {
        font-size: 1.125rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        flex-shrink: 0;
      }

      /* ─── Rows ───────────────────────────────────────────────────────────── */

      .lpb__group-body {
        display: flex;
        flex-direction: column;
      }

      .lpb__row {
        display: flex;
        align-items: center;
        gap: 0.5rem;
        padding: 0.625rem 1rem;
        border-top: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        background: var(--tch-color-surface, #fffbff);
        min-height: 3.5rem;
      }

      .lpb__row--active {
        background: var(--tch-color-surface, #fffbff);
      }

      .lpb__row-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 0.125rem;
      }

      .lpb__row-label {
        font-size: 0.8125rem;
        font-weight: 500;
        color: var(--tch-color-on-surface, #1a1c1e);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .lpb__row-value {
        font-size: 0.75rem;
        font-weight: 600;
        color: var(--tch-color-primary, #745b00);
        font-variant-numeric: tabular-nums;
      }

      .lpb__row-inherited {
        font-size: 0.75rem;
        color: var(--tch-color-on-surface-variant, #46464f);
        font-style: italic;
      }

      .lpb__row-empty {
        font-size: 0.75rem;
        color: var(--tch-color-outline, #787680);
      }

      .lpb__row-actions {
        display: flex;
        gap: 0;
        flex-shrink: 0;
      }

      .lpb__action-btn {
        width: 2rem;
        height: 2rem;
        font-size: 1rem;
        color: var(--tch-color-on-surface-variant, #46464f);
      }

      .lpb__action-btn--delete {
        color: var(--tch-color-error, #ba1a1a);
      }

      /* ─── Responsive: 2-column grid from 600dp ───────────────────────────── */

      @include ui.up(medium) {
        .lpb__group-body {
          display: grid;
          grid-template-columns: 1fr 1fr;
        }

        .lpb__row {
          border-top: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        }

        .lpb__row:nth-child(odd) {
          border-right: 1px solid var(--tch-color-outline-variant, #c8c5d0);
        }
      }
    `,
  ],
})
export class LimitPolicyBlockComponent {
  readonly specs = input<LimitBlockSpec[]>([]);
  readonly assignments = input<LimitBlockAssignment[]>([]);
  readonly inheritedAssignments = input<LimitBlockAssignment[] | null>(null);
  readonly inheritedScopeLabel = input<string>('tenant');
  readonly defaultExpandedGroups = input<LimitGroup[]>([]);

  readonly editRequested = output<LimitPolicyEditRequest>();
  readonly deleteRequested = output<string>();

  private readonly expandedState = signal<Set<LimitGroup>>(new Set());

  readonly sections = computed<GroupSection[]>(() => {
    const specs = this.specs();
    const assignments = this.assignments();
    const inherited = this.inheritedAssignments() ?? [];
    const defaultExpanded = this.defaultExpandedGroups();

    // Initialize expanded state lazily from defaultExpandedGroups
    if (this.expandedState().size === 0 && defaultExpanded.length > 0) {
      this.expandedState.set(new Set(defaultExpanded));
    }

    const assignmentMap = new Map(assignments.map(a => [a.ruleKey, a]));
    const inheritedMap = new Map(inherited.map(a => [a.ruleKey, a]));
    const specMap = new Map(specs.map(s => [s.ruleKey, s]));

    return GROUPS.map(groupId => {
      const ruleKeys = GROUP_RULES[groupId];
      const rows: GroupRow[] = ruleKeys
        .map(ruleKey => {
          const spec = specMap.get(ruleKey);
          if (!spec) return null;
          const assignment = assignmentMap.get(ruleKey) ?? null;
          const inheritedAssignment = inheritedMap.get(ruleKey) ?? null;
          return {
            spec,
            assignment,
            inheritedAssignment,
            displayValue: assignment ? formatAssignmentParams(assignment) : null,
            inheritedDisplayValue: inheritedAssignment
              ? formatAssignmentParams(inheritedAssignment)
              : null,
          } satisfies GroupRow;
        })
        .filter((r): r is GroupRow => r !== null);

      return { id: groupId, label: GROUP_LABELS[groupId], rows };
    }).filter(g => g.rows.length > 0);
  });

  isExpanded(groupId: LimitGroup): boolean {
    return this.expandedState().has(groupId);
  }

  activeCount(group: GroupSection): number {
    return group.rows.filter(r => r.assignment !== null).length;
  }

  toggleGroup(groupId: LimitGroup): void {
    this.expandedState.update(set => {
      const next = new Set(set);
      if (next.has(groupId)) {
        next.delete(groupId);
      } else {
        next.add(groupId);
      }
      return next;
    });
  }

  onEdit(row: GroupRow): void {
    this.editRequested.emit({
      spec: row.spec,
      assignment: row.assignment,
      inheritedAssignment: row.inheritedAssignment,
    });
  }

  onDelete(row: GroupRow): void {
    if (row.assignment) {
      this.deleteRequested.emit(row.assignment.id.value);
    }
  }
}
