import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { TranslatePipe } from '@ngx-translate/core';
import { TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { AdminStatusTone, TchIdentityCardComponent, TchIdentityCardMeta } from '@tch/ui/console';
import {
  ConsoleEntityDetailActionEvent,
  ConsoleEntityDetailComponent,
  ConsoleFact,
  ConsoleDrawResultCombinationsComponent,
  ConsoleDrawResultRawComponent,
  ConsoleDrawResultSummaryComponent,
  ConsoleDrawResultSummaryFacts,
  ConsoleDrawResultSummaryView,
  ConsoleDrawSlotIdentity,
  DrawCombinationGameSection,
  consoleDrawResultSummaryFacts,
  consoleDrawResultSummaryViewModel,
  consoleDrawResultQualityLabel,
  consoleDrawResultStatusLabel,
  consoleDrawResultStatusTone,
  drawCombinationGameSectionsFromResult,
} from '@tch/web/console';

import {
  AdminDrawResultsApi,
  DrawResultQuality,
  DrawResultStatus,
  DrawResultView,
} from '.././data-access/admin-draw-results-api.service';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-result-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ConsoleEntityDetailComponent,
    TchIdentityCardComponent,
    MatTabsModule,
    TranslatePipe,
    ConsoleDrawResultSummaryComponent,
    ConsoleDrawResultCombinationsComponent,
    ConsoleDrawResultRawComponent,
  ],
  templateUrl: './admin-draw-result-detail.page.html',
  styleUrls: ['./admin-draw-result-detail.page.scss'],
})
export class AdminDrawResultDetailPage implements OnInit {
  private readonly api = inject(AdminDrawResultsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly pageState = signal<PageState>('loading');
  readonly result = signal<DrawResultView | null>(null);
  readonly errorTitle = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  /** Winning combinations per supported game, derived from the drawn numbers. */
  readonly combinationSections = computed<readonly DrawCombinationGameSection[]>(() => {
    const result = this.result();
    if (!result) return [];
    return drawCombinationGameSectionsFromResult(result);
  });

  readonly rawResult = computed<string | null>(() => {
    const result = this.result();
    if (!result) return null;
    const payload = result.rawPayload ?? result.sourceResult ?? result.haitiResult ?? null;
    return payload ? JSON.stringify(payload, null, 2) : null;
  });
  readonly summaryView = computed<ConsoleDrawResultSummaryView | null>(() => {
    const result = this.result();
    if (!result) return null;
    return consoleDrawResultSummaryViewModel({
      identityInput: {
        providerCode: result.provider,
        channelCode: result.channelCode,
        channelName: result.channelName,
        slotKey: result.slotKey,
        slotLabel: result.slotLabel,
      },
      numbers: result.numbers ?? [],
    });
  });
  readonly summaryFacts = computed<ConsoleDrawResultSummaryFacts>(() =>
    consoleDrawResultSummaryFacts({
      resultFacts: this.resultFacts(),
      linkedDrawFacts: this.linkedDrawFacts(),
    }),
  );

  readonly drawIdentity = computed<ConsoleDrawSlotIdentity | null>(
    () => this.summaryView()?.identity ?? null,
  );
  readonly title = computed(() => {
    const identity = this.drawIdentity();
    return firstText(
      identity?.providerShortName,
      identity?.providerCode,
      identity?.channelShortName,
      identity?.slotLabel,
      'Détail du résultat',
    );
  });
  readonly description = computed(() => {
    const result = this.result();
    if (!result) return 'Consultez le résultat appliqué à ce tirage.';
    const identity = this.drawIdentity();
    return [
      firstText(
        identity?.providerName,
        identity?.channelName,
        result.channelName,
        result.provider,
        'Tirage',
      ),
      firstText(identity?.slotLabel),
      result.drawDate ?? result.resultDate ?? '—',
    ]
      .filter(Boolean)
      .join(' · ');
  });
  readonly detailMeta = computed(() => {
    const result = this.result();
    const identity = this.drawIdentity();
    return result
      ? [
          firstText(identity?.slotKey, result.slotKey, '—'),
          result.drawDate ?? result.resultDate ?? '—',
          result.status,
        ]
      : [];
  });
  readonly detailError = computed(() =>
    this.errorTitle()
      ? { title: this.errorTitle() ?? 'Problème détecté', message: this.errorMessage() ?? '' }
      : null,
  );
  readonly detailActions = computed(() => {
    const actions = [{ id: 'back', label: 'Tirages', icon: 'arrow_back' }];
    if (this.result()?.drawId) {
      actions.push({ id: 'draw', label: 'Détail du tirage', icon: 'event' });
    }
    return actions;
  });

  readonly identityMeta = computed<readonly TchIdentityCardMeta[]>(() => {
    const result = this.result();
    if (!result) return [];
    return [
      { label: 'Statut', value: this.statusLabel(result.status) },
      { label: 'Qualité', value: this.qualityLabel(result.quality) },
      { label: 'Tirage', value: result.drawDate ?? result.resultDate ?? '—' },
      {
        label: 'Slot',
        value: this.drawIdentity()?.slotLabel ?? result.slotLabel ?? result.slotKey ?? '—',
      },
    ];
  });
  readonly resultFacts = computed<readonly ConsoleFact[]>(() => {
    const result = this.result();
    if (!result) return [];
    return [
      { label: 'Statut', value: this.statusLabel(result.status) },
      { label: 'Qualité', value: this.qualityLabel(result.quality) },
      {
        label: 'Appliqué le',
        value: result.appliedAt ? this.formatDate(result.appliedAt) : 'Non appliqué',
      },
      {
        label: 'Publié le',
        value: result.publishedAt ? this.formatDate(result.publishedAt) : 'Non publié',
      },
    ];
  });
  readonly linkedDrawFacts = computed<readonly ConsoleFact[]>(() => {
    const result = this.result();
    if (!result) return [];
    return [
      { label: 'Slot', value: result.slotKey ?? '—', code: true },
      { label: 'Créneau', value: result.slotLabel ?? '—' },
      { label: 'Date', value: result.drawDate ?? result.resultDate ?? '—' },
      {
        label: 'Heure officielle',
        value: result.occurredAt ? this.formatDate(result.occurredAt) : 'Non disponible',
      },
      {
        label: this.resultTimestampTitle(result),
        value: this.resultTimestampValue(result),
      },
    ];
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const resultId = this.route.snapshot.paramMap.get('resultId');
    const slotKey = this.route.snapshot.queryParamMap.get('slotKey') ?? undefined;
    const drawDate = this.route.snapshot.queryParamMap.get('drawDate') ?? undefined;

    if (!resultId) {
      this.setError('Résultat introuvable', 'Aucun identifiant de résultat n’a été fourni.');
      return;
    }

    this.pageState.set('loading');
    this.errorTitle.set(null);
    this.errorMessage.set(null);

    this.api
      .list(
        {
          slotKey,
          from: drawDate,
          to: drawDate,
          size: 100,
          sort: 'occurredAt,DESC',
        },
        { suppressShellFeedback: true },
      )
      .subscribe({
        next: page => {
          const result = page.items.find(item => item.id === resultId);
          if (!result) {
            this.setError(
              'Résultat introuvable',
              'Le résultat n’a pas été trouvé pour ce tirage. Revenez au détail du tirage ou rafraîchissez la page.',
            );
            return;
          }
          this.result.set(result);
          this.pageState.set('ready');
        },
        error: err => {
          const error = this.errorViewModel(err, 'admin.drawResults.detail');
          this.setError(error.title, error.message);
        },
      });
  }

  providerCode(result: DrawResultView): string {
    return (
      result.provider?.toUpperCase() ??
      result.channelCode?.toUpperCase() ??
      result.slotKey?.toUpperCase() ??
      '—'
    );
  }

  statusLabel(status: DrawResultStatus): string {
    return consoleDrawResultStatusLabel(status);
  }

  qualityLabel(quality: DrawResultQuality): string {
    return consoleDrawResultQualityLabel(quality);
  }

  resultTimestampTitle(result: DrawResultView): string {
    return result.source === 'MANUAL' ? 'Saisi le' : 'Récupéré le';
  }

  resultTimestampValue(result: DrawResultView): string {
    const timestamp = result.fetchedAt ?? result.publishedAt ?? result.appliedAt;
    return timestamp ? this.formatDate(timestamp) : 'Non disponible';
  }

  onDetailAction(event: ConsoleEntityDetailActionEvent): void {
    switch (event.action.id) {
      case 'back':
        void this.router.navigate(['/app/admin/draws']);
        break;
      case 'draw':
        if (this.result()?.drawId) {
          void this.router.navigate(['/app/admin/draws', this.result()!.drawId]);
        }
        break;
    }
  }

  statusTone(status: DrawResultStatus): AdminStatusTone {
    return consoleDrawResultStatusTone(status);
  }

  private setError(title: string, message: string): void {
    this.pageState.set('error');
    this.errorTitle.set(title);
    this.errorMessage.set(message);
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(
      mapHttpErrorToProblemDetail(err),
      source,
      'page',
    );
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }

  private formatDate(value: string): string {
    return new Intl.DateTimeFormat('fr-CA', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }
}

function firstText(...values: readonly (string | null | undefined)[]): string {
  return values.find(value => typeof value === 'string' && value.trim().length > 0)?.trim() ?? '';
}
