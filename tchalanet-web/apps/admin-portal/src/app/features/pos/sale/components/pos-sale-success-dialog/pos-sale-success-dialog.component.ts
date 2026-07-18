import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';

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
    TranslatePipe,
  ],
  templateUrl: './pos-sale-success-dialog.component.html',
  styleUrls: ['./pos-sale-success-dialog.component.scss'],
})
export class PosSaleSuccessDialogComponent implements OnInit {
  private readonly api = inject(PosSaleApiService);
  private readonly translate = inject(TranslateService);
  private readonly dialogRef = inject(MatDialogRef<PosSaleSuccessDialogComponent, PosSaleSuccessDialogResult>);
  protected readonly data = inject<PosSaleSuccessDialogData>(MAT_DIALOG_DATA);

  readonly phoneNumber = signal('');
  readonly email = signal('');
  readonly selectedDeliveryChannel = signal<DeliveryChannel | null>(null);
  readonly printing = signal(false);
  readonly sending = signal<DeliveryChannel | null>(null);
  readonly communicationAvailability = signal<PosSaleActionAvailabilityView | null>(null);
  readonly feedback = signal<{ type: 'success' | 'error'; message: string } | null>(null);

  readonly titleKey = computed(() =>
    this.data.mode === 'ticket-actions'
      ? 'admin.pos.dialog.title.ticketActions'
      : 'admin.pos.dialog.title.saleSuccess',
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
  readonly availableDeliveryChannels = computed<readonly DeliveryChannel[]>(() => {
    const channels: DeliveryChannel[] = [];
    if (this.canSendSms()) channels.push('SMS');
    if (this.canSendWhatsapp()) channels.push('WHATSAPP');
    if (this.canSendEmail()) channels.push('EMAIL');
    return channels;
  });
  readonly activeDeliveryChannel = computed<DeliveryChannel | null>(() => {
    const selected = this.selectedDeliveryChannel();
    const availableChannels = this.availableDeliveryChannels();
    if (selected && availableChannels.includes(selected)) return selected;
    return availableChannels[0] ?? null;
  });
  readonly deliveryRecipient = computed(() => {
    const channel = this.activeDeliveryChannel();
    if (!channel) return '';
    return this.recipientFor(channel).trim();
  });
  readonly canSendSelectedDelivery = computed(() => {
    const channel = this.activeDeliveryChannel();
    return !!channel && !this.sending() && this.isValidRecipient(channel, this.deliveryRecipient());
  });

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

  selectDeliveryChannel(channel: DeliveryChannel): void {
    if (!this.availableDeliveryChannels().includes(channel)) return;
    if (this.activeDeliveryChannel() !== channel) {
      this.phoneNumber.set('');
      this.email.set('');
      this.feedback.set(null);
    }
    this.selectedDeliveryChannel.set(channel);
  }

  deliveryChannelLabelKey(channel: DeliveryChannel): string {
    return `admin.pos.dialog.delivery.channel.${channel.toLowerCase()}`;
  }

  deliveryChannelIcon(channel: DeliveryChannel): string {
    if (channel === 'SMS') return 'sms';
    if (channel === 'WHATSAPP') return 'chat';
    return 'mail';
  }

  deliveryFieldLabelKey(channel: DeliveryChannel): string {
    return channel === 'EMAIL'
      ? 'admin.pos.dialog.delivery.email'
      : 'admin.pos.dialog.delivery.phone';
  }

  deliveryFieldPlaceholderKey(channel: DeliveryChannel): string {
    return channel === 'EMAIL'
      ? 'admin.pos.dialog.delivery.emailPlaceholder'
      : 'admin.pos.dialog.delivery.phonePlaceholder';
  }

  deliveryInputType(channel: DeliveryChannel): string {
    return channel === 'EMAIL' ? 'email' : 'tel';
  }

  deliveryInputMode(channel: DeliveryChannel): string {
    return channel === 'EMAIL' ? 'email' : 'tel';
  }

  recipientFor(channel: DeliveryChannel): string {
    return channel === 'EMAIL' ? this.email() : this.phoneNumber();
  }

  setRecipientFor(channel: DeliveryChannel, value: string): void {
    if (channel === 'EMAIL') {
      this.email.set(value);
    } else {
      this.phoneNumber.set(value);
    }
  }

  print(): void {
    if (this.printing()) return;
    this.printing.set(true);
    this.feedback.set(null);

    this.api.printTicket(this.data.ticket.ticketId, this.data.sellerTerminalId, {
      recordPrint: true,
      reprintReason: this.data.mode === 'ticket-actions' ? 'POS reprint' : 'POS print',
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
        this.feedback.set({
          type: 'success',
          message: this.translate.instant('admin.pos.dialog.feedback.printSuccess'),
        });
      },
      error: () => {
        this.printing.set(false);
        this.feedback.set({
          type: 'error',
          message: this.translate.instant('admin.pos.dialog.feedback.printError'),
        });
      },
    });
  }

  sendSelectedDelivery(): void {
    const channel = this.activeDeliveryChannel();
    const to = this.deliveryRecipient();
    if (!channel || !this.isValidRecipient(channel, to) || this.sending()) return;

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
        this.feedback.set({
          type: 'success',
          message: this.translate.instant('admin.pos.dialog.feedback.sendSuccess'),
        });
      },
      error: () => {
        this.sending.set(null);
        this.feedback.set({
          type: 'error',
          message: this.translate.instant('admin.pos.dialog.feedback.sendError'),
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

  private isValidRecipient(channel: DeliveryChannel, to: string): boolean {
    if (!to) return false;
    if (channel !== 'EMAIL') return to.replace(/\D/g, '').length >= 8;
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(to);
  }
}
