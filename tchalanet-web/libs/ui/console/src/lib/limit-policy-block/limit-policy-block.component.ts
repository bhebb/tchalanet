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

export interface LpbLabels {
  unconfigured: string;
  configured: string;
  inheritedFrom: string;
  editAria: string;
  deleteAria: string;
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

const GROUPS: LimitGroup[] = ['VENTE', 'RESTRICTIONS', 'EXPOSITION'];

function formatAssignmentParams(
  assignment: LimitBlockAssignment,
  configuredLabel: string,
): string {
  const p = assignment.params as Record<string, unknown> | null;
  if (!p) return configuredLabel;
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
  return configuredLabel;
}

@Component({
  selector: 'tch-limit-policy-block',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './limit-policy-block.component.html',
  styleUrl: './limit-policy-block.component.scss',
})
export class LimitPolicyBlockComponent {
  readonly specs = input.required<LimitBlockSpec[]>();
  readonly assignments = input.required<LimitBlockAssignment[]>();
  readonly inheritedAssignments = input<LimitBlockAssignment[] | null>();
  readonly inheritedScopeLabel = input<string>();
  readonly defaultExpandedGroups = input<LimitGroup[]>();
  readonly groupLabels = input.required<Record<LimitGroup, string>>();
  readonly labels = input.required<LpbLabels>();

  readonly editRequested = output<LimitPolicyEditRequest>();
  readonly deleteRequested = output<string>();

  private readonly userToggles = signal<Set<LimitGroup>>(new Set());

  private readonly expandedState = computed<Set<LimitGroup>>(() => {
    const result = new Set(this.defaultExpandedGroups() ?? []);
    for (const g of this.userToggles()) {
      if (result.has(g)) result.delete(g);
      else result.add(g);
    }
    return result;
  });

  readonly sections = computed<GroupSection[]>(() => {
    const specs = this.specs();
    const assignments = this.assignments();
    const inherited = this.inheritedAssignments() ?? [];
    const gl = this.groupLabels();
    const configuredLabel = this.labels().configured;

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
            displayValue: assignment
              ? formatAssignmentParams(assignment, configuredLabel)
              : null,
            inheritedDisplayValue: inheritedAssignment
              ? formatAssignmentParams(inheritedAssignment, configuredLabel)
              : null,
          } satisfies GroupRow;
        })
        .filter((r): r is GroupRow => r !== null);

      return { id: groupId, label: gl[groupId], rows };
    }).filter(g => g.rows.length > 0);
  });

  isExpanded(groupId: LimitGroup): boolean {
    return this.expandedState().has(groupId);
  }

  activeCount(group: GroupSection): number {
    return group.rows.filter(r => r.assignment !== null).length;
  }

  toggleGroup(groupId: LimitGroup): void {
    this.userToggles.update(set => {
      const next = new Set(set);
      if (next.has(groupId)) next.delete(groupId);
      else next.add(groupId);
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
