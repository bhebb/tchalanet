import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel } from '@tch/ui/components';

import { resolveErrorFeedbackCopy } from '@tch/web/errors';
import { ErrorViewModel, toErrorViewModel } from '@tch/web/errors';
import { AdminPageShellComponent } from '../../../shared/admin-ui/admin-page-shell.component';
import { AdminSectionCardComponent } from '../../../shared/admin-ui/admin-section-card.component';
import {
  AdminTicketsApi,
  AdminTicketLineRequest,
  AdminTicketPreviewView,
  AdminSoldTicketView,
} from '../../admin-tickets-api.service';
import { AdminDrawsApi, DrawSummaryView } from '../../admin-draws-api.service';
import { SellerTerminalApi, SellerTerminalSummaryRow } from '../../seller-terminal-api.service';

type PageState = 'idle' | 'previewing' | 'previewed' | 'selling' | 'sold' | 'error';

const BET_TYPES = ['DIRECT', 'PLACE', 'EACH_WAY'] as const;
const GAME_CODES = ['BORLETTE', 'LOTTO3', 'TCHALA'] as const;

@Component({
  selector: 'tch-admin-sell-ticket-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    RouterLink,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
  ],
  templateUrl: './admin-sell-ticket.page.html',
  styleUrls: ['./admin-sell-ticket.page.scss'],
})
export class AdminSellTicketPage implements OnInit {
  private readonly api = inject(AdminTicketsApi);
  private readonly drawsApi = inject(AdminDrawsApi);
  private readonly terminalsApi = inject(SellerTerminalApi);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly state = signal<PageState>('idle');
  readonly terminals = signal<SellerTerminalSummaryRow[]>([]);
  readonly draws = signal<DrawSummaryView[]>([]);
  readonly preview = signal<AdminTicketPreviewView | null>(null);
  readonly sold = signal<AdminSoldTicketView | null>(null);
  readonly loadError = signal<ErrorViewModel | null>(null);
  readonly actionError = signal<ErrorViewModel | null>(null);

  readonly betTypes = BET_TYPES;
  readonly gameCodes = GAME_CODES;

  readonly form = this.fb.nonNullable.group({
    terminalId: ['', Validators.required],
    drawId: ['', Validators.required],
    currency: ['HTG', Validators.required],
    lines: this.fb.array([this.buildLine()]),
  });

  get lines(): FormArray<FormGroup> {
    return this.form.controls.lines as FormArray<FormGroup>;
  }

  ngOnInit(): void {
    this.loadSelectors();
  }

  private loadSelectors(): void {
    this.loadError.set(null);
    this.terminalsApi.list({ status: 'ACTIVE', size: 100 }, { suppressShellFeedback: true }).subscribe({
      next: p => this.terminals.set(p.items),
      error: err => this.loadError.set(this.errorViewModel(err, 'admin.support.sell.terminals')),
    });

    this.drawsApi.listToday({ size: 50 }, { suppressShellFeedback: true }).subscribe({
      next: p => {
        const today = p.content.filter(d => d.status === 'OPEN' || d.status === 'SCHEDULED');
        this.draws.set(today);
      },
      error: err => this.loadError.set(this.errorViewModel(err, 'admin.support.sell.draws.today')),
    });

    this.drawsApi.listUpcoming({ days: 2, size: 50 }, { suppressShellFeedback: true }).subscribe({
      next: p => {
        const upcoming = p.content.filter(d => d.status === 'OPEN' || d.status === 'SCHEDULED');
        this.draws.update(existing => {
          const ids = new Set(existing.map(d => d.id));
          return [...existing, ...upcoming.filter(d => !ids.has(d.id))];
        });
      },
      error: err => {
        if (this.draws().length === 0) {
          this.loadError.set(this.errorViewModel(err, 'admin.support.sell.draws.upcoming'));
        }
      },
    });
  }

  buildLine(): FormGroup {
    return this.fb.nonNullable.group({
      gameCode: ['BORLETTE', Validators.required],
      betType: ['DIRECT', Validators.required],
      selection: ['', Validators.required],
      betOption: [1, [Validators.required, Validators.min(1), Validators.max(4)]],
      stake: [null as number | null, [Validators.required, Validators.min(1)]],
    });
  }

  addLine(): void {
    this.lines.push(this.buildLine());
  }

  removeLine(i: number): void {
    if (this.lines.length > 1) this.lines.removeAt(i);
  }

  runPreview(): void {
    if (this.form.invalid) return;
    this.state.set('previewing');
    this.preview.set(null);
    this.actionError.set(null);

    const raw = this.form.getRawValue();
    this.api.preview({
      terminalId: raw.terminalId,
      drawId: raw.drawId,
      currency: raw.currency,
      lines: raw.lines as AdminTicketLineRequest[],
    }, { suppressShellFeedback: true }).subscribe({
      next: result => {
        this.preview.set(result);
        this.state.set('previewed');
      },
      error: (err: unknown) => {
        this.actionError.set(this.errorViewModel(err, 'admin.support.sell.preview'));
        this.state.set('idle');
      },
    });
  }

  confirmSell(): void {
    if (this.form.invalid || !this.preview()) return;
    this.state.set('selling');
    this.actionError.set(null);

    const raw = this.form.getRawValue();
    this.api.sell({
      terminalId: raw.terminalId,
      drawId: raw.drawId,
      currency: raw.currency,
      lines: raw.lines as AdminTicketLineRequest[],
    }, { suppressShellFeedback: true }).subscribe({
      next: result => {
        this.sold.set(result);
        this.state.set('sold');
      },
      error: (err: unknown) => {
        this.actionError.set(this.errorViewModel(err, 'admin.support.sell.confirm'));
        this.state.set('previewed');
      },
    });
  }

  reset(): void {
    this.form.reset({ currency: 'HTG' });
    while (this.lines.length > 1) this.lines.removeAt(1);
    this.lines.at(0).reset({ gameCode: 'BORLETTE', betType: 'DIRECT', betOption: 1 });
    this.preview.set(null);
    this.sold.set(null);
    this.actionError.set(null);
    this.state.set('idle');
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  drawLabel(d: DrawSummaryView): string {
    return `${d.channel.name} — ${d.drawDate} ${d.slot.label} (${d.status})`;
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const problem = (err as { error?: ProblemDetail })?.error;
    if (problem) {
      const normalized = webAppErrorFromProblemDetail(problem, source, 'page');
      const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
      return toErrorViewModel(normalized, copy);
    }

    return {
      title: this.translate.instant('common.errors.fallback.title'),
      message: this.translate.instant('common.errors.fallback.message'),
      severity: 'error',
    };
  }
}
