import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import {
  AdminSectionCardComponent,
  AdminStatusTone,
  TchIdentityCardComponent,
  TchIdentityCardMeta,
} from '@tch/ui/console';
import {
  ConsoleEntityDetailActionEvent,
  ConsoleEntityDetailComponent,
  ConsoleFact,
  ConsoleFactsComponent,
} from '@tch/web/console';

import {
  AdminDrawResultsApi,
  DrawResultQuality,
  DrawResultStatus,
  DrawResultView,
} from '../../../data-access/admin-draw-results-api.service';
import { lotteryLogoForSlot, lotteryProviderCodeFromSlot } from '../../../../../shared/lottery/lottery-assets';

type PageState = 'loading' | 'ready' | 'error';

@Component({
  selector: 'tch-admin-draw-result-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    ConsoleEntityDetailComponent,
    ConsoleFactsComponent,
    AdminSectionCardComponent,
    TchIdentityCardComponent,
  ],
  templateUrl: './admin-draw-result-detail.page.html',
  styleUrls: ['./admin-draw-result-detail.page.scss'],
})
export class AdminDrawResultDetailPage implements OnInit {
  private readonly api = inject(AdminDrawResultsApi);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly pageState = signal<PageState>('loading');
  readonly result = signal<DrawResultView | null>(null);
  readonly errorTitle = signal<string | null>(null);
  readonly errorMessage = signal<string | null>(null);

  readonly title = computed(() => this.result()?.slotLabel ?? this.result()?.slotKey ?? 'Détail du résultat');
  readonly description = computed(() => {
    const result = this.result();
    if (!result) return 'Consultez le résultat appliqué à ce tirage.';
    return `${result.channelName ?? result.provider ?? 'Tirage'} · ${result.drawDate ?? result.resultDate ?? '—'}`;
  });
  readonly detailMeta = computed(() => {
    const result = this.result();
    return result
      ? [result.slotKey ?? '—', result.drawDate ?? result.resultDate ?? '—', result.status]
      : [];
  });
  readonly detailError = computed(() =>
    this.errorTitle()
      ? { title: this.errorTitle() ?? 'Problème détecté', message: this.errorMessage() ?? '' }
      : null,
  );
  readonly detailActions = computed(() => {
    const actions = [
      { id: 'back', label: 'Tirages', icon: 'arrow_back' },
    ];
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
      { label: 'Slot', value: result.slotKey ?? '—' },
    ];
  });
  readonly resultFacts = computed<readonly ConsoleFact[]>(() => {
    const result = this.result();
    if (!result) return [];
    return [
      { label: 'Statut', value: this.statusLabel(result.status) },
      { label: 'Qualité', value: this.qualityLabel(result.quality) },
      { label: 'Appliqué le', value: result.appliedAt ? this.formatDate(result.appliedAt) : 'Non appliqué' },
      { label: 'Publié le', value: result.publishedAt ? this.formatDate(result.publishedAt) : 'Non publié' },
    ];
  });
  readonly linkedDrawFacts = computed<readonly ConsoleFact[]>(() => {
    const result = this.result();
    if (!result) return [];
    return [
      { label: 'Slot', value: result.slotKey ?? '—', code: true },
      { label: 'Créneau', value: result.slotLabel ?? '—' },
      { label: 'Date', value: result.drawDate ?? result.resultDate ?? '—' },
      { label: 'Heure officielle', value: result.occurredAt ? this.formatDate(result.occurredAt) : 'Non disponible' },
      { label: 'Récupéré le', value: result.fetchedAt ? this.formatDate(result.fetchedAt) : 'Non disponible' },
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

    this.api.list({
      slotKey,
      from: drawDate,
      to: drawDate,
      size: 100,
      sort: 'occurredAt,DESC',
    }, { suppressShellFeedback: true }).subscribe({
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
      error: () => {
        this.setError('Impossible de charger le résultat', 'Réessayez ou revenez au détail du tirage.');
      },
    });
  }

  providerLogo(result: DrawResultView): string | null {
    return lotteryLogoForSlot(result.slotKey ?? result.provider);
  }

  providerCode(result: DrawResultView): string {
    return (
      lotteryProviderCodeFromSlot(result.slotKey)?.toUpperCase() ??
      result.provider?.toUpperCase() ??
      result.channelCode?.toUpperCase() ??
      '—'
    );
  }

  statusLabel(status: DrawResultStatus): string {
    switch (status) {
      case 'CONFIRMED': return 'Confirmé';
      case 'PROVISIONAL': return 'Provisoire';
      case 'OVERRIDDEN': return 'Remplacé';
      case 'ERROR': return 'Erreur';
      case 'APPLIED': return 'Appliqué';
      case 'CORRECTED': return 'Corrigé';
      case 'VOIDED': return 'Annulé';
      case 'PENDING': return 'En attente';
    }
  }

  qualityLabel(quality: DrawResultQuality): string {
    switch (quality) {
      case 'COMPLETE': return 'Complet';
      case 'SUSPECT': return 'À vérifier';
      case 'INVALID': return 'Invalide';
      case 'OFFICIAL': return 'Officiel';
      case 'MANUAL': return 'Manuel';
      case 'ESTIMATED': return 'Estimé';
      case 'UNKNOWN': return 'Inconnu';
    }
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
    switch (status) {
      case 'CONFIRMED':
      case 'APPLIED': return 'success';
      case 'PROVISIONAL':
      case 'CORRECTED': return 'warning';
      case 'ERROR':
      case 'VOIDED': return 'danger';
      case 'PENDING': return 'neutral';
      case 'OVERRIDDEN': return 'warning';
    }
  }

  private setError(title: string, message: string): void {
    this.pageState.set('error');
    this.errorTitle.set(title);
    this.errorMessage.set(message);
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
