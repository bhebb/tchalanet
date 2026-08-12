import { COMMA, ENTER } from '@angular/cdk/keycodes';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import type { MatChipInputEvent } from '@angular/material/chips';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TranslatePipe } from '@ngx-translate/core';

import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchSectionError } from '@tch/ui/components';
import { AdminDialogShellComponent } from '@tch/ui/console';
import { resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';
import { TranslateService } from '@ngx-translate/core';

import { DrawAdminApi } from '../../../draws/data-access/draw-admin.api.service';
import type { DrawSummary } from '../../../draws/data-access/draw-admin.api.service';
import { AdminLimitsApi } from '../../data-access/admin-limits-api.service';
import type { LimitRuleSpec } from '../../data-access/admin-limits.models';
import { buildParams, detectParamSchema } from '../../data-access/admin-limits.models';

type DurationMode = 'today' | 'permanent';

@Component({
  selector: 'tch-block-number-quick-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    AdminDialogShellComponent,
    MatButtonModule,
    MatButtonToggleModule,
    MatChipsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    TchSectionError,
    TranslatePipe,
  ],
  templateUrl: './block-number-quick-dialog.component.html',
})
export class BlockNumberQuickDialogComponent implements OnInit {
  private readonly drawApi = inject(DrawAdminApi);
  private readonly limitsApi = inject(AdminLimitsApi);
  private readonly dialogRef = inject(MatDialogRef<BlockNumberQuickDialogComponent>);
  private readonly translate = inject(TranslateService);

  readonly separatorKeyCodes = [ENTER, COMMA];

  spec: LimitRuleSpec | null = null;

  readonly loadingDraws = signal(true);
  readonly openDraws = signal<DrawSummary[]>([]);
  readonly selectedDrawId = signal<string | null>(null);
  readonly numbers = signal<string[]>([]);
  readonly duration = signal<DurationMode>('today');
  readonly saving = signal(false);
  readonly errorMessage = signal<string | null>(null);

  ngOnInit(): void {
    this.drawApi.listDraws({ status: 'OPEN', size: 30 }).subscribe({
      next: page => {
        this.openDraws.set(page.items ?? []);
        const first = (page.items ?? [])[0];
        if (first) this.selectedDrawId.set(first.id);
        this.loadingDraws.set(false);
      },
      error: () => {
        this.loadingDraws.set(false);
      },
    });
  }

  drawLabel(draw: DrawSummary): string {
    return `${draw.channel.name} ${draw.slot.label}`;
  }

  addNumber(event: MatChipInputEvent): void {
    const val = (event.value ?? '').trim();
    if (val && !this.numbers().includes(val)) {
      this.numbers.update(n => [...n, val]);
    }
    event.chipInput?.clear();
  }

  removeNumber(n: string): void {
    this.numbers.update(list => list.filter(x => x !== n));
  }

  get canSave(): boolean {
    return this.numbers().length > 0 && this.selectedDrawId() !== null && !this.saving();
  }

  save(): void {
    if (!this.canSave) return;

    const draw = this.openDraws().find(d => d.id === this.selectedDrawId());
    if (!draw) return;

    const spec = this.spec;
    const params = spec
      ? buildParams(detectParamSchema(spec), spec.paramsTemplate, {
          valueCentsHtg: 0,
          maxCount: 0,
          windowMinutes: 0,
          betTypeCode: '',
          selectionIds: this.numbers(),
        })
      : { selections: this.numbers() };

    const endsAt = this.duration() === 'today' ? this.todayEnd() : null;

    this.saving.set(true);
    this.errorMessage.set(null);

    this.limitsApi
      .upsertAssignment(
        {
          ruleKey: 'BLOCK_SELECTION_PER_DRAW',
          targetType: 'DRAW_CHANNEL',
          targetId: draw.channel.id,
          enabled: true,
          onBreach: 'BLOCK',
          params,
          startsAt: null,
          endsAt,
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.dialogRef.close(true);
        },
        error: (err: unknown) => {
          this.saving.set(false);
          const problem = mapHttpErrorToProblemDetail(err);
          const normalized = webAppErrorFromProblemDetail(problem, 'admin.limits.blockNumber', 'section');
          const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
          this.errorMessage.set(toErrorViewModel(normalized, copy).message);
        },
      });
  }

  private todayEnd(): string {
    const d = new Date();
    d.setHours(23, 59, 59, 999);
    return d.toISOString();
  }
}
