import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTabsModule } from '@angular/material/tabs';
import { forkJoin } from 'rxjs';

import { AdminPageShellComponent } from '../../../../shared/admin-ui/admin-page-shell.component';
import { PlatformCommunicationApi } from '../../data-access/platform-communication-api.service';
import {
  PlatformRecipientPickerComponent,
  PlatformRecipientPickerSelection,
} from '../../../shared/recipient-picker/platform-recipient-picker.component';

@Component({
  selector: 'tch-platform-communication-tests-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    AdminPageShellComponent,
    PlatformRecipientPickerComponent,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTabsModule,
  ],
  templateUrl: './platform-communication-tests.page.html',
  styleUrls: ['./platform-communication.page.scss'],
})
export class PlatformCommunicationTestsPage {
  private readonly api = inject(PlatformCommunicationApi);
  private readonly fb = inject(FormBuilder);
  private readonly snackBar = inject(MatSnackBar);

  readonly testing = signal<string | null>(null);
  readonly recipientSelection = signal<PlatformRecipientPickerSelection | null>(null);

  readonly testForm = this.fb.nonNullable.group({
    slackChannelKey: ['ops-alerts'],
    emailTo: [''],
    phoneTo: [''],
    whatsappTo: [''],
    title: ['Tchalanet communication test'],
    message: ['Message de test envoyé depuis la console superadmin.'],
  });

  sendSlackTest(): void {
    const v = this.testForm.getRawValue();
    this.runProviderTest('SLACK', () => this.api.testSlack({
      channelKey: v.slackChannelKey,
      title: v.title,
      message: v.message,
    }));
  }

  sendEmailTest(): void {
    const v = this.testForm.getRawValue();
    const recipients = this.selectedEmails();
    if (recipients.length > 0) {
      this.runProviderBatchTest('EMAIL', recipients, to => this.api.testEmail({
        to,
        subject: v.title,
        message: v.message,
      }));
      return;
    }
    this.runProviderTest('EMAIL', () => this.api.testEmail({
      to: v.emailTo || '',
      subject: v.title,
      message: v.message,
    }));
  }

  sendSmsTest(): void {
    const v = this.testForm.getRawValue();
    const recipients = this.selectedPhones();
    if (recipients.length > 0) {
      this.runProviderBatchTest('SMS', recipients, to => this.api.testSms({
        to,
        title: v.title,
        message: v.message,
      }));
      return;
    }
    this.runProviderTest('SMS', () => this.api.testSms({
      to: v.phoneTo || '',
      title: v.title,
      message: v.message,
    }));
  }

  sendWhatsappTest(): void {
    const v = this.testForm.getRawValue();
    const recipients = this.selectedPhones();
    if (recipients.length > 0) {
      this.runProviderBatchTest('WHATSAPP', recipients, to => this.api.testWhatsapp({
        to,
        title: v.title,
        message: v.message,
      }));
      return;
    }
    this.runProviderTest('WHATSAPP', () => this.api.testWhatsapp({
      to: v.whatsappTo || '',
      title: v.title,
      message: v.message,
    }));
  }

  updateRecipients(selection: PlatformRecipientPickerSelection): void {
    this.recipientSelection.set(selection);
  }

  private selectedEmails(): string[] {
    return Array.from(new Set(
      (this.recipientSelection()?.recipients ?? [])
        .map(recipient => recipient.email?.trim())
        .filter((value): value is string => !!value),
    ));
  }

  private selectedPhones(): string[] {
    return Array.from(new Set(
      (this.recipientSelection()?.recipients ?? [])
        .map(recipient => recipient.phone?.trim())
        .filter((value): value is string => !!value),
    ));
  }

  private runProviderTest(
    channel: string,
    request: () => ReturnType<PlatformCommunicationApi['testSlack']>,
  ): void {
    this.testing.set(channel);
    request().subscribe({
      next: result => {
        this.testing.set(null);
        const status = result.sent ? 'envoyé' : `non envoyé (${result.reason || 'dégradé'})`;
        this.snackBar.open(`${result.channel}: ${status}`, 'OK', { duration: 7000 });
      },
      error: (err: unknown) => {
        this.testing.set(null);
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? `${channel}: erreur d'envoi.`,
          'OK',
          { duration: 7000 },
        );
      },
    });
  }

  private runProviderBatchTest(
    channel: string,
    recipients: string[],
    request: (recipient: string) => ReturnType<PlatformCommunicationApi['testSlack']>,
  ): void {
    this.testing.set(channel);
    forkJoin(recipients.map(recipient => request(recipient))).subscribe({
      next: results => {
        this.testing.set(null);
        const sent = results.filter(result => result.sent).length;
        this.snackBar.open(`${channel}: ${sent}/${results.length} message(s) envoyés.`, 'OK', { duration: 7000 });
      },
      error: (err: unknown) => {
        this.testing.set(null);
        this.snackBar.open(
          (err as { error?: { title?: string } })?.error?.title ?? `${channel}: erreur d'envoi.`,
          'OK',
          { duration: 7000 },
        );
      },
    });
  }
}
