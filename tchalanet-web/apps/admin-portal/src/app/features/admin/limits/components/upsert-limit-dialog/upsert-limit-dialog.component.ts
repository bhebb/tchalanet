import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { forkJoin } from 'rxjs';

import { webAppErrorFromProblemDetail } from '@tch/api';
import type { ProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';

import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { BreachOutcome, LimitAssignmentItem, LimitRuleSpec, TargetType } from '../../data-access/admin-limits.models';
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
    MatCheckboxModule,
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
  private readonly destroyRef = inject(DestroyRef);

  readonly breachOutcomes = V0_BREACH_OUTCOMES;

  readonly spec = signal<LimitRuleSpec | null>(null);
  readonly assignment = signal<LimitAssignmentItem | null>(null);
  readonly targetType = signal<TargetType>('TENANT');
  readonly targetId = signal<string | null>(null);
  readonly saving = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly paramSchema = signal<ParamSchema>('NONE');
  readonly onBreachValue = signal<BreachOutcome>('BLOCK');

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
  readonly showSelectionId = computed(() => this.paramSchema() === 'SELECTION');
  readonly showAutoWarn = computed(() =>
    this.showValueCents() && this.onBreachValue() === 'BLOCK',
  );

  readonly form = this.fb.nonNullable.group({
    onBreach: ['BLOCK' as BreachOutcome, [Validators.required]],
    enabled: [true],
    valueCentsHtg: [0 as number],
    maxCount: [0 as number],
    windowMinutes: [0 as number],
    betTypeCode: [''],
    selectionId: [''],
    autoWarn: [false],
    warnThresholdHtg: [0 as number],
  });

  constructor() {
    this.form.controls.onBreach.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => this.onBreachValue.set(v));
  }

  init(
    spec: LimitRuleSpec,
    targetType: TargetType,
    targetId: string | null,
    assignment: LimitAssignmentItem | null,
  ): void {
    this.spec.set(spec);
    this.targetType.set(targetType);
    this.targetId.set(targetId);
    this.assignment.set(assignment);

    const schema = detectParamSchema(spec);
    this.paramSchema.set(schema);
    this.applyValidators(schema);

    const srcParams = assignment?.params ?? spec.paramsTemplate;
    const extracted = extractParamValues(schema, spec.paramsTemplate, srcParams);

    const onBreach = assignment?.onBreach ?? spec.defaultOutcome;
    this.form.patchValue({
      onBreach,
      enabled: assignment?.enabled ?? true,
      ...extracted,
      autoWarn: false,
      warnThresholdHtg: Math.max(0, extracted.valueCentsHtg - 1000),
    });
    this.onBreachValue.set(onBreach);
    this.error.set(null);
    this.saving.set(false);
  }

  save(): void {
    if (this.form.invalid || this.saving()) return;
    const spec = this.spec();
    if (!spec) return;

    const v = this.form.getRawValue();
    const params = buildParams(this.paramSchema(), spec.paramsTemplate, v);

    const mainReq = {
      ruleKey: spec.ruleKey,
      targetType: this.targetType(),
      targetId: this.targetId() ?? undefined,
      enabled: v.enabled,
      onBreach: v.onBreach,
      params,
    };

    const requests = [this.api.upsertAssignment(mainReq, { suppressShellFeedback: true })];

    if (v.autoWarn && this.showAutoWarn() && v.warnThresholdHtg > 0) {
      const warnParams = buildParams(this.paramSchema(), spec.paramsTemplate, {
        ...v,
        valueCentsHtg: v.warnThresholdHtg,
      });
      requests.push(
        this.api.upsertAssignment(
          { ...mainReq, onBreach: 'WARN', params: warnParams },
          { suppressShellFeedback: true },
        ),
      );
    }

    this.saving.set(true);
    this.error.set(null);

    forkJoin(requests).subscribe({
      next: ([result]) => this.ref.close(result),
      error: (err: unknown) => {
        this.error.set(this.resolveError(err, `admin.limits.upsert.${spec.ruleKey}`));
        this.saving.set(false);
      },
    });
  }

  private applyValidators(schema: ParamSchema): void {
    const c = this.form.controls;
    c.valueCentsHtg.clearValidators();
    c.maxCount.clearValidators();
    c.windowMinutes.clearValidators();
    c.betTypeCode.clearValidators();
    c.selectionId.clearValidators();

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
      case 'SELECTION':
        c.selectionId.addValidators([Validators.required]);
        break;
    }

    c.valueCentsHtg.updateValueAndValidity({ emitEvent: false });
    c.maxCount.updateValueAndValidity({ emitEvent: false });
    c.windowMinutes.updateValueAndValidity({ emitEvent: false });
    c.betTypeCode.updateValueAndValidity({ emitEvent: false });
    c.selectionId.updateValueAndValidity({ emitEvent: false });
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
