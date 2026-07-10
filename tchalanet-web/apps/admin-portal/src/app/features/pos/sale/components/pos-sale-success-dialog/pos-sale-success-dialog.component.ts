import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { PosSaleActionAvailabilityView, PosTicketBackupView } from '../../data-access/pos-sale.models';
import { PosSaleApiService } from '../../data-access/pos-sale-api.service';

export interface PosTicketDialogTicket {
  ticketId: string;
  ticketCode: string;
  publicCode?: string | null;
  backup?: PosTicketBackupView | null;
  actionAvailability?: PosSaleActionAvailabilityView | null;
}

export interface PosSaleSuccessDialogData {
  ticket: PosTicketDialogTicket;
  sellerTerminalId: string;
  totalAmount?: number | null;
  currency?: string | null;
  lineCount?: number | null;
  freeLineCount?: number | null;
  mode?: 'sale-success' | 'ticket-actions';
}

export type PosSaleSuccessDialogResult = 'new-ticket' | 'details' | undefined;

type DeliveryChannel = 'SMS' | 'WHATSAPP' | 'EMAIL';

@Component({
  selector: 'tch-pos-sale-success-dialog',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    FormsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './pos-sale-success-dialog.component.html',
  styleUrls: ['./pos-sale-success-dialog.component.scss'],
})
export class PosSaleSuccessDialogComponent implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly dialogRef = inject(MatDialogRef<PosSaleSuccessDialogComponent, PosSaleSuccessDialogResult>);
  protected readonly data = inject<PosSaleSuccessDialogData>(MAT_DIALOG_DATA);

  readonly phoneNumber = signal('');
  readonly email = signal('');
  readonly printing = signal(false);
  readonly sending = signal<DeliveryChannel | null>(null);
  readonly communicationAvailability = signal<PosSaleActionAvailabilityView | null>(null);
  readonly feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  readonly title = computed(() =>
    this.data.mode === 'ticket-actions' ? 'Impression du ticket' : 'Ticket vendu',
  );

  readonly titleIcon = computed(() =>
    this.data.mode === 'ticket-actions' ? 'print' : 'verified',
  );

  readonly actionAvailability = computed(() =>
    this.communicationAvailability() ??
    this.data.ticket.actionAvailability ?? {
      canSell: false,
      canPrint: true,
      canSendSms: false,
      canSendWhatsapp: false,
      canSendEmail: false,
      canCopy: true,
    },
  );

  readonly publicDisplayCode = computed(() =>
    this.data.ticket.publicCode || this.data.ticket.backup?.displayCode || this.data.ticket.ticketCode,
  );

  readonly canSendSms = computed(() => this.actionAvailability().canSendSms);
  readonly canSendWhatsapp = computed(() => this.actionAvailability().canSendWhatsapp);
  readonly canSendEmail = computed(() => this.actionAvailability().canSendEmail);
  readonly hasDeliveryActions = computed(() =>
    this.canSendSms() || this.canSendWhatsapp() || this.canSendEmail(),
  );

  readonly currency = computed(() => this.data.currency ?? 'HTG');
  readonly showSaleActions = computed(() => this.data.mode !== 'ticket-actions');

  ngOnInit(): void {
    this.api.getTicketCommunicationAvailability({ suppressShellFeedback: true }).subscribe({
      next: availability => this.communicationAvailability.set({
        ...availability,
        canPrint: this.data.ticket.actionAvailability?.canPrint ?? availability.canPrint,
      }),
      error: () => {
        this.communicationAvailability.set(null);
      },
    });
  }

  print(): void {
    if (this.printing()) return;
    this.printing.set(true);
    this.feedback.set(null);

    this.api.printTicket(this.data.ticket.ticketId, this.data.sellerTerminalId, {
      recordPrint: this.data.mode === 'ticket-actions' ? false : true,
      reprintReason: this.data.mode === 'ticket-actions' ? 'Admin ticket reprint' : 'POS print',
    }).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const opened = window.open(url, '_blank', 'noopener,noreferrer');

        if (!opened) {
          const link = document.createElement('a');
          link.href = url;
          link.download = `${this.data.ticket.ticketCode}.pdf`;
          link.click();
        }

        window.setTimeout(() => URL.revokeObjectURL(url), 60_000);
        this.printing.set(false);
        this.feedback.set({ type: 'success', message: 'Reçu prêt pour impression.' });
      },
      error: () => {
        this.printing.set(false);
        this.feedback.set({
          type: 'error',
          message: 'Le ticket est vendu, mais le reçu n’a pas pu être imprimé.',
        });
      },
    });
  }

  send(channel: DeliveryChannel): void {
    const to = channel === 'EMAIL' ? this.email().trim() : this.phoneNumber().trim();
    if (!to || this.sending()) return;

    this.sending.set(channel);
    this.feedback.set(null);
    this.api.sendTicketReceipt(
      this.data.ticket.ticketId,
      this.data.sellerTerminalId,
      channel,
      to,
    ).subscribe({
      next: () => {
        this.sending.set(null);
        this.feedback.set({ type: 'success', message: 'Envoi demandé.' });
      },
      error: () => {
        this.sending.set(null);
        this.feedback.set({
          type: 'error',
          message: 'L’envoi n’a pas pu être demandé. Vérifiez le destinataire et réessayez.',
        });
      },
    });
  }

  newTicket(): void {
    this.dialogRef.close('new-ticket');
  }

  details(): void {
    this.dialogRef.close('details');
  }
}
