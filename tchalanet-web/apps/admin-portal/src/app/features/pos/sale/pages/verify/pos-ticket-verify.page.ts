import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { TranslateService } from '@ngx-translate/core';
import { mapHttpErrorToProblemDetail, webAppErrorFromProblemDetail } from '@tch/api';
import { TchErrorPanel, TchLoading, TchNotice } from '@tch/ui/components';
import { AdminEmptyStateComponent, AdminPageShellComponent, AdminSectionCardComponent } from '@tch/ui/console';
import { ErrorViewModel, resolveErrorFeedbackCopy, toErrorViewModel } from '@tch/web/errors';

import { PosSaleApiService } from '../../data-access/pos-sale-api.service';
import {
  PosSellerTerminalPickerView,
  PosTicketVerificationView,
} from '../../data-access/pos-sale.models';

@Component({
  selector: 'tch-pos-ticket-verify-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DatePipe,
    RouterLink,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    AdminEmptyStateComponent,
    AdminPageShellComponent,
    AdminSectionCardComponent,
    TchErrorPanel,
    TchLoading,
    TchNotice,
  ],
  templateUrl: './pos-ticket-verify.page.html',
  styleUrls: ['./pos-ticket-verify.page.scss'],
})
export class PosTicketVerifyPage implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly translate = inject(TranslateService);

  readonly loadingTerminals = signal(false);
  readonly verifying = signal(false);
  readonly pageError = signal<ErrorViewModel | null>(null);
  readonly verifyError = signal<ErrorViewModel | null>(null);
  readonly sellerTerminals = signal<PosSellerTerminalPickerView[]>([]);
  readonly selectedSellerTerminalId = signal('');
  readonly scannedValue = signal('');
  readonly result = signal<PosTicketVerificationView | null>(null);
  readonly verifiedAt = signal<Date | null>(null);

  readonly selectedSellerTerminal = computed(() =>
    this.sellerTerminals().find(t => t.sellerTerminalId === this.selectedSellerTerminalId()) ?? null,
  );

  readonly canVerify = computed(() =>
    !!this.selectedSellerTerminalId() &&
    this.scannedValue().trim().length > 0 &&
    !this.verifying(),
  );

  ngOnInit(): void {
    this.loadSellerTerminals();
  }

  loadSellerTerminals(): void {
    this.loadingTerminals.set(true);
    this.pageError.set(null);

    this.api.listSellerTerminalsForSale(
      { status: 'ACTIVE', page: 0, size: 100, sort: 'displayName,asc' },
      { suppressShellFeedback: true },
    ).subscribe({
      next: page => {
        this.sellerTerminals.set(page.items);
        const requestedTerminalId = this.route.snapshot.queryParamMap.get('sellerTerminalId');
        this.selectedSellerTerminalId.set(
          page.items.find(t => t.sellerTerminalId === requestedTerminalId)?.sellerTerminalId
          ?? page.items[0]?.sellerTerminalId
          ?? '',
        );
        this.loadingTerminals.set(false);
      },
      error: err => {
        this.pageError.set(this.errorViewModel(err, 'admin.pos.ticket.verify.terminals'));
        this.sellerTerminals.set([]);
        this.loadingTerminals.set(false);
      },
    });
  }

  onSellerTerminalChange(value: string): void {
    this.selectedSellerTerminalId.set(value);
    this.result.set(null);
    this.verifyError.set(null);
  }

  onScannedValueInput(event: Event): void {
    const value = (event.target as HTMLInputElement).value;
    this.scannedValue.set(value);
    this.result.set(null);
    this.verifyError.set(null);
  }

  verify(): void {
    if (!this.canVerify()) return;

    this.verifying.set(true);
    this.verifyError.set(null);
    this.result.set(null);

    this.api.verifyTicket(
      this.scannedValue().trim(),
      this.selectedSellerTerminalId(),
      { suppressShellFeedback: true },
    ).subscribe({
      next: result => {
        this.result.set(result);
        this.verifiedAt.set(new Date());
        this.verifying.set(false);
      },
      error: err => {
        this.verifyError.set(this.errorViewModel(err, 'admin.pos.ticket.verify'));
        this.verifying.set(false);
      },
    });
  }

  resultTone(result: PosTicketVerificationView): 'success' | 'warning' | 'error' | 'info' {
    if (result.severity === 'SUCCESS') return 'success';
    if (result.severity === 'WARNING') return 'warning';
    if (result.severity === 'ERROR') return 'error';
    return 'info';
  }

  resultTitle(result: PosTicketVerificationView): string {
    switch (result.status) {
      case 'PAYABLE':
        return 'Ticket gagnant à payer';
      case 'ALREADY_PAID':
        return 'Ticket déjà payé';
      case 'NOT_PAYABLE_LOST':
        return 'Ticket non gagnant';
      case 'NOT_PAYABLE_PENDING_DRAW':
        return 'Tirage pas encore effectué';
      case 'NOT_PAYABLE_RESULT_PENDING':
        return 'Résultat en attente';
      case 'CANCELLED':
        return 'Ticket annulé';
      case 'VOIDED':
        return 'Ticket invalidé';
      case 'NOT_FOUND':
        return 'Ticket introuvable';
      case 'REPAIR_REQUIRED':
        return 'Ticket à vérifier';
      case 'OPERATION_NOT_ALLOWED':
        return 'Vérification non autorisée';
      default:
        return 'Vérification terminée';
    }
  }

  resultMessage(result: PosTicketVerificationView): string {
    const amount = this.param(result, 'amount');
    const currency = this.param(result, 'currency') || 'HTG';

    switch (result.status) {
      case 'PAYABLE':
        return amount
          ? `Ce ticket peut être payé. Montant à payer : ${amount} ${currency}.`
          : 'Ce ticket peut être payé.';
      case 'ALREADY_PAID':
        return 'Ce ticket a déjà été payé. Ne le payez pas une deuxième fois.';
      case 'NOT_PAYABLE_LOST':
        return 'Ce ticket ne donne pas droit à un paiement.';
      case 'NOT_PAYABLE_PENDING_DRAW':
        return 'Le tirage n’est pas encore terminé. Vérifiez après le résultat.';
      case 'NOT_PAYABLE_RESULT_PENDING':
        return 'Le tirage est terminé, mais le résultat n’est pas encore disponible.';
      case 'CANCELLED':
      case 'VOIDED':
        return 'Ce ticket ne doit pas être payé.';
      case 'NOT_FOUND':
        return 'Aucun ticket ne correspond au code saisi.';
      case 'REPAIR_REQUIRED':
        return 'Le ticket doit être vérifié par un responsable avant paiement.';
      case 'OPERATION_NOT_ALLOWED':
        return 'Sélectionnez un terminal vendeur actif avant de vérifier.';
      default:
        return 'Vérifiez le statut avant toute action.';
    }
  }

  verificationCodeDisplay(result: PosTicketVerificationView): string {
    const fallback = this.scannedValue().trim();
    return this.param(result, 'displayCode')
      ?? this.param(result, 'publicCode')
      ?? this.param(result, 'ticketCode')
      ?? (fallback || '—');
  }

  param(result: PosTicketVerificationView, key: string): string | null {
    const value = result.params[key];
    return value == null ? null : String(value);
  }

  private errorViewModel(err: unknown, source: string): ErrorViewModel {
    const normalized = webAppErrorFromProblemDetail(mapHttpErrorToProblemDetail(err), source, 'page');
    const copy = resolveErrorFeedbackCopy(normalized, key => this.translate.instant(key));
    return toErrorViewModel(normalized, copy);
  }
}
