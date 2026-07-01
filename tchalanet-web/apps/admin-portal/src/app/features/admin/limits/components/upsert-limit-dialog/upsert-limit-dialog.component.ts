import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { COMMA, ENTER } from '@angular/cdk/keycodes';
import type { MatChipInputEvent } from '@angular/material/chips';
import { TranslateService } from '@ngx-translate/core';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { RuntimeSettingsStore } from '@tch/shared-config';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { BreachOutcome, LimitAssignmentItem, LimitRuleSpec, RuleKey, TargetType } from '../../data-access/admin-limits.models';
import {
  ParamSchema,
  buildParams,
  detectParamSchema,
  extractParamValues,
} from '../../data-access/admin-limits.models';

const V0_BREACH_OUTCOMES: BreachOutcome[] = ['BLOCK', 'WARN'];

@Component({
  selector: 'tch-upsert-limit-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatButtonToggleModule,
    MatCheckboxModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    TchSectionError,
  ],
  templateUrl: './upsert-limit-dialog.component.html',
  styleUrl: './upsert-limit-dialog.component.scss',
})
export class UpsertLimitDialogComponent {
  private readonly api = inject(AdminLimitsApi);
  private readonly ref = inject(MatDialogRef<UpsertLimitDialogComponent>);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);
  private readonly runtimeSettings = inject(RuntimeSettingsStore);

  readonly breachOutcomes = V0_BREACH_OUTCOMES;
  readonly separatorKeyCodes: number[] = [ENTER, COMMA];

  // 'add' = user picks rule first; 'edit' = rule is fixed
  readonly mode = signal<'add' | 'edit'>('edit');
  readonly availableRules = signal<LimitRuleSpec[]>([]);

  readonly spec = signal<LimitRuleSpec | null>(null);
  readonly assignment = signal<LimitAssignmentItem | null>(null);
  readonly targetType = signal<TargetType>('TENANT');
  readonly targetId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly paramSchema = signal<ParamSchema>('NONE');
  readonly selections = signal<string[]>([]);
  readonly durationMode = signal<'permanent' | 'today' | 'custom'>('permanent');
  readonly customEndsAt = signal<string>('');

  readonly showParamForm = computed(() => this.spec() !== null);
  readonly showValueCents = computed(() =>
    this.paramSchema() === 'CENTS' || this.paramSchema() === 'CENTS_BET_TYPE',
  );
  readonly showMaxCount = computed(() =>
    this.paramSchema() === 'COUNT' || this.paramSchema() === 'WINDOW_COUNT',
  );
  readonly showWindowMinutes = computed(() => this.paramSchema() === 'WINDOW_COUNT');
  readonly showBetTypeCode = computed(() =>
    this.paramSchema() === 'BET_TYPE' || this.paramSchema() === 'CENTS_BET_TYPE',
  );
  readonly showSelectionChips = computed(() => this.paramSchema() === 'SELECTION');
  readonly showCustomEndDate = computed(() => this.durationMode() === 'custom');
  readonly timezone = computed(() => {
    const value = this.runtimeSettings.settings().values['app.timezone'];
    return typeof value === 'string' && value.trim() ? value : 'America/Port-au-Prince';
  });
  readonly canSave = computed(() =>
    !this.saving() &&
    this.spec() !== null &&
    this.form.valid &&
    (this.paramSchema() !== 'SELECTION' || this.selections().length > 0) &&
    (this.durationMode() !== 'custom' || this.customEndsAt().length > 0),
  );

  readonly form = this.fb.nonNullable.group({
    onBreach: ['BLOCK' as BreachOutcome, [Validators.required]],
    enabled: [true],
    valueCentsHtg: [0 as number],
    maxCount: [0 as number],
    windowMinutes: [0 as number],
    betTypeCode: [''],
  });

  /** Edit existing assignment. */
  init(
    spec: LimitRuleSpec,
    targetType: TargetType,
    targetId: string | null,
    assignment: LimitAssignmentItem | null,
  ): void {
    this.mode.set('edit');
    this.targetType.set(targetType);
    this.targetId.set(targetId);
    this.assignment.set(assignment);
    this.error.set(null);
    this.saving.set(false);
    this.applySpec(spec, assignment);
  }

  /**
   * Add new assignment: user must first pick the rule from `unassignedRules`.
   * Call this when no row.assignment exists yet and user clicks "Ajouter".
   */
  initAdd(
    unassignedRules: LimitRuleSpec[],
    targetType: TargetType,
    targetId: string | null,
  ): void {
    this.mode.set('add');
    this.availableRules.set(unassignedRules);
    this.targetType.set(targetType);
    this.targetId.set(targetId);
    this.assignment.set(null);
    this.spec.set(null);
    this.paramSchema.set('NONE');
    this.selections.set([]);
    this.durationMode.set('permanent');
    this.customEndsAt.set('');
    this.error.set(null);
    this.saving.set(false);
  }

  onRuleSelected(ruleKey: RuleKey): void {
    const rule = this.availableRules().find(r => r.ruleKey === ruleKey);
    if (rule) this.applySpec(rule, null);
  }

  addSelection(event: MatChipInputEvent): void {
    const val = (event.value ?? '').trim();
    if (val && !this.selections().includes(val)) {
      this.selections.update(s => [...s, val]);
    }
    event.chipInput?.clear();
  }

  removeSelection(sel: string): void {
    this.selections.update(s => s.filter(x => x !== sel));
  }

  save(): void {
    const spec = this.spec();
    if (!this.canSave() || !spec) return;
    const v = this.form.getRawValue();
    const params = buildParams(this.paramSchema(), spec.paramsTemplate, {
      valueCentsHtg: v.valueCentsHtg,
      maxCount: v.maxCount,
      windowMinutes: v.windowMinutes,
      betTypeCode: v.betTypeCode,
      selectionIds: this.selections(),
    });

    const req = {
      ruleKey: spec.ruleKey,
      targetType: this.targetType(),
      targetId: this.targetId() ?? undefined,
      enabled: v.enabled,
      onBreach: v.onBreach,
      params,
      startsAt: null,
      endsAt: this.buildEndsAt(),
    };

    this.saving.set(true);
    this.error.set(null);

    this.api.upsertAssignment(req, { suppressShellFeedback: true }).subscribe({
      next: result => this.ref.close(result),
      error: (err: unknown) => {
        this.error.set(this.resolveError(err, `admin.limits.upsert.${spec.ruleKey}`));
        this.saving.set(false);
      },
    });
  }

  onCustomEndsAtChange(event: Event): void {
    this.customEndsAt.set((event.target as HTMLInputElement).value);
  }

  private buildEndsAt(): string | null {
    const mode = this.durationMode();
    if (mode === 'permanent') return null;
    const timezone = this.timezone();
    if (mode === 'today') {
      const today = getDatePartsInTimeZone(new Date(), timezone);
      return endOfDayIsoInTimeZone(today, timezone);
    }
    const dateStr = this.customEndsAt();
    if (!dateStr) return null;
    const [y, m, d] = dateStr.split('-').map(Number);
    return endOfDayIsoInTimeZone({ year: y, month: m, day: d }, timezone);
  }

  private applySpec(spec: LimitRuleSpec, assignment: LimitAssignmentItem | null): void {
    this.spec.set(spec);
    this.assignment.set(assignment);
    const schema = detectParamSchema(spec);
    this.paramSchema.set(schema);
    this.applyValidators(schema);

    const srcParams = assignment?.params ?? spec.paramsTemplate;
    const extracted = extractParamValues(schema, spec.paramsTemplate, srcParams);

    this.selections.set(extracted.selectionIds);

    const endsAt = assignment?.endsAt ?? null;
    if (endsAt) {
      this.durationMode.set('custom');
      this.customEndsAt.set(endsAt.substring(0, 10));
    } else {
      this.durationMode.set('permanent');
      this.customEndsAt.set('');
    }

    const onBreach = assignment?.onBreach ?? spec.defaultOutcome;
    this.form.patchValue({
      onBreach,
      enabled: assignment?.enabled ?? true,
      valueCentsHtg: extracted.valueCentsHtg,
      maxCount: extracted.maxCount,
      windowMinutes: extracted.windowMinutes,
      betTypeCode: extracted.betTypeCode,
    });
  }

  private applyValidators(schema: ParamSchema): void {
    const c = this.form.controls;
    c.valueCentsHtg.clearValidators();
    c.maxCount.clearValidators();
    c.windowMinutes.clearValidators();
    c.betTypeCode.clearValidators();

    switch (schema) {
      case 'CENTS':
        c.valueCentsHtg.addValidators([Validators.required, Validators.min(0.01)]);
        break;
      case 'CENTS_BET_TYPE':
        c.valueCentsHtg.addValidators([Validators.required, Validators.min(0.01)]);
        c.betTypeCode.addValidators([Validators.required]);
        break;
      case 'COUNT':
        c.maxCount.addValidators([Validators.required, Validators.min(1)]);
        break;
      case 'WINDOW_COUNT':
        c.maxCount.addValidators([Validators.required, Validators.min(1)]);
        c.windowMinutes.addValidators([Validators.required, Validators.min(1)]);
        break;
      case 'BET_TYPE':
        c.betTypeCode.addValidators([Validators.required]);
        break;
    }

    c.valueCentsHtg.updateValueAndValidity({ emitEvent: false });
    c.maxCount.updateValueAndValidity({ emitEvent: false });
    c.windowMinutes.updateValueAndValidity({ emitEvent: false });
    c.betTypeCode.updateValueAndValidity({ emitEvent: false });
  }

  private resolveError(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (!problem) {
      return {
        severity: 'error',
        title: this.translate.instant('common.errors.fallback.title'),
        message: this.translate.instant('common.errors.fallback.message'),
      };
    }
    const normalized = webAppErrorFromProblemDetail(problem, source, 'section');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}

interface DateParts {
  readonly year: number;
  readonly month: number;
  readonly day: number;
}

function getDatePartsInTimeZone(date: Date, timeZone: string): DateParts {
  const parts = new Intl.DateTimeFormat('en-CA', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(date);
  return {
    year: Number(parts.find(p => p.type === 'year')?.value),
    month: Number(parts.find(p => p.type === 'month')?.value),
    day: Number(parts.find(p => p.type === 'day')?.value),
  };
}

function endOfDayIsoInTimeZone(parts: DateParts, timeZone: string): string {
  const guess = new Date(Date.UTC(parts.year, parts.month - 1, parts.day, 23, 59, 59, 0));
  const offset = getTimeZoneOffsetMs(guess, timeZone);
  return new Date(guess.getTime() - offset).toISOString();
}

function getTimeZoneOffsetMs(date: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    hourCycle: 'h23',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  }).formatToParts(date);
  const get = (type: Intl.DateTimeFormatPartTypes): number =>
    Number(parts.find(p => p.type === type)?.value);
  const asUtc = Date.UTC(
    get('year'),
    get('month') - 1,
    get('day'),
    get('hour'),
    get('minute'),
    get('second'),
  );
  return asUtc - date.getTime();
}
