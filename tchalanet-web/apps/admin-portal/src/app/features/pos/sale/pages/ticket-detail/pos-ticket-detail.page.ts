import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateService } from '@ngx-translate/core';
import { ProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading } from '@tch/ui/components';
import { AdminPageShellComponent, AdminSectionCardComponent, AdminStatusPillComponent, AdminStatusTone } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { PosSaleApiService } from '../../data-access/pos-sale-api.service';
import { PosTicketDetailsView } from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-ticket-detail-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    AdminStatusPillComponent,
    TchErrorPanel,
    TchLoading,
  ],
  templateUrl: './pos-ticket-detail.page.html',
  styleUrls: ['./pos-ticket-detail.page.scss'],
})
export class PosTicketDetailPage implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly loading = signal(false);
  readonly error = signal<ErrorViewModel | null>(null);
  readonly ticket = signal<PosTicketDetailsView | null>(null);

  readonly title = computed(() => {
    const ticket = this.ticket();
    return ticket ? `Ticket ${ticket.ticketCode}` : 'Détail du ticket';
  });

  readonly description = computed(() => {
    const ticket = this.ticket();
    return ticket
      ? `${ticket.drawChannelName} · ${this.amountDisplay(ticket.totalAmountCents)} ${ticket.currency}`
      : 'Consultez les informations de vente, de tirage et de lignes du ticket.';
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    const ticketId = this.route.snapshot.paramMap.get('ticketId');
    if (!ticketId) {
      this.error.set({
        title: 'Ticket introuvable',
        message: 'Aucun identifiant de ticket n’a été fourni.',
        severity: 'error',
      });
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.api.getTicketDetails(ticketId, { suppressShellFeedback: true }).subscribe({
      next: ticket => {
        this.ticket.set(ticket);
        this.loading.set(false);
      },
      error: err => {
        this.error.set(this.errorViewModel(err, 'admin.pos.ticket.detail'));
        this.loading.set(false);
      },
    });
  }

  amountDisplay(cents: number): string {
    return (cents / 100).toFixed(2);
  }

  statusLabel(status: string): string {
    switch (status) {
      case 'APPROVED':
        return 'Approuvé';
      case 'PENDING_APPROVAL':
        return 'En approbation';
      case 'REJECTED':
        return 'Rejeté';
      case 'PAID':
        return 'Payé';
      case 'CANCELLED':
        return 'Annulé';
      case 'VOIDED':
        return 'Invalidé';
      case 'EXPIRED':
        return 'Expiré';
      default:
        return status || 'Statut inconnu';
    }
  }

  statusTone(status: string): AdminStatusTone {
    switch (status) {
      case 'APPROVED':
      case 'PAID':
        return 'success';
      case 'PENDING_APPROVAL':
      case 'EXPIRED':
        return 'warning';
      case 'REJECTED':
      case 'CANCELLED':
      case 'VOIDED':
        return 'danger';
      default:
        return 'neutral';
    }
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
