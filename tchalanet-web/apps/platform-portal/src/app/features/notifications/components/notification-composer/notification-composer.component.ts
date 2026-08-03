import { Component, computed, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTabsModule } from '@angular/material/tabs';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TchSectionError } from '@tch/ui/components';
import { ErrorViewModel } from '@tch/web/errors';

import {
    PlatformRecipientPickerComponent,
    PlatformRecipientPickerSelection,
} from '../../../shared/recipient-picker/platform-recipient-picker.component';
import {
    NotificationAudienceType,
    NotificationCategory,
    NotificationChannel,
    NotificationSeverity,
    NotificationTarget,
} from '../../data-access/notifications-api.service';

type TargetMode = 'BROADCAST' | 'SPECIFIC';

export interface NotificationDraftEvent {
    payload: {
        sourceType: string;
        sourceId: null;
        dedupeKey: null;
        audienceType: NotificationAudienceType;
        targets: NotificationTarget[] | undefined;
        severity: NotificationSeverity;
        kind: 'SYSTEM_ERROR' | 'WARNING';
        category: NotificationCategory;
        titleText: string;
        messageText: string;
        translations: {
            fr: { title: string; body: string };
            en: { title: string; body: string };
            ht: { title: string; body: string };
        };
        actionType: 'LINK' | null;
        actionUrl: string | null;
        payload: { to: string; email: string; phone: string; whatsapp: string } | null;
        expiresAt: null;
        channels: NotificationChannel[];
    };
    tenantId: string | null;
}

const SEVERITY_OPTIONS: NotificationSeverity[] = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];
const CATEGORY_OPTIONS: NotificationCategory[] = ['SYSTEM', 'SECURITY', 'BATCH', 'TENANT_CONFIG', 'DRAW', 'RESULT'];
const AUDIENCE_OPTIONS: NotificationAudienceType[] = ['PLATFORM_ADMINS', 'ALL_APP_USERS'];
const CHANNEL_OPTIONS: NotificationChannel[] = ['IN_APP', 'SLACK', 'EMAIL', 'SMS', 'WHATSAPP'];

@Component({
    selector: 'tch-notification-composer',
    imports: [
        ReactiveFormsModule,
        TchSectionError,
        PlatformRecipientPickerComponent,
        MatButtonModule,
        MatFormFieldModule,
        MatIconModule,
        MatInputModule,
        MatSelectModule,
        MatTabsModule,
        MatTooltipModule,
    ],
    templateUrl: './notification-composer.component.html',
    styleUrl: './notification-composer.component.scss',
})
export class NotificationComposerComponent {
    readonly saving = input<boolean>(false);
    readonly apiError = input<ErrorViewModel | null>(null);

    readonly submitted = output<NotificationDraftEvent>();
    readonly cancelled = output<void>();

    readonly severityOptions = SEVERITY_OPTIONS;
    readonly categoryOptions = CATEGORY_OPTIONS;
    readonly audienceOptions = AUDIENCE_OPTIONS;
    readonly channelOptions = CHANNEL_OPTIONS;

    readonly localError = signal<ErrorViewModel | null>(null);
    readonly displayError = computed(() => this.localError() ?? this.apiError());

    private readonly recipientSelection = signal<PlatformRecipientPickerSelection | null>(null);

    private readonly fb = inject(FormBuilder);
    readonly form = this.fb.nonNullable.group({
        titleFr: ['', [Validators.required, Validators.maxLength(160)]],
        bodyFr: ['', [Validators.required, Validators.maxLength(1000)]],
        titleEn: ['', [Validators.required, Validators.maxLength(160)]],
        bodyEn: ['', [Validators.required, Validators.maxLength(1000)]],
        titleHt: ['', [Validators.required, Validators.maxLength(160)]],
        bodyHt: ['', [Validators.required, Validators.maxLength(1000)]],
        severity: ['WARNING' as NotificationSeverity],
        category: ['SYSTEM' as NotificationCategory],
        targetMode: ['BROADCAST' as TargetMode],
        audienceType: ['PLATFORM_ADMINS' as NotificationAudienceType],
        actionUrl: [''],
        channels: [['IN_APP'] as NotificationChannel[]],
        externalDestination: [''],
    });

    onUpdateRecipients(selection: PlatformRecipientPickerSelection): void {
        this.recipientSelection.set(selection);
        this.localError.set(null);
    }

    submit(): void {
        if (this.form.invalid) {
            this.form.markAllAsTouched();
            return;
        }

        const value = this.form.getRawValue();
        const externalDestination = value.externalDestination.trim();
        const recipientSelection = this.recipientSelection();
        const specificTargets = value.targetMode === 'SPECIFIC' ? recipientSelection?.targets ?? [] : [];

        if (value.targetMode === 'SPECIFIC' && specificTargets.length === 0) {
            this.localError.set({
                title: 'Destinataire requis',
                message: 'Sélectionnez au moins un destinataire avant d’envoyer la notification.',
                severity: 'error',
            });
            return;
        }

        this.localError.set(null);

        this.submitted.emit({
            payload: {
                sourceType: 'platform-admin',
                sourceId: null,
                dedupeKey: null,
                audienceType: value.targetMode === 'SPECIFIC'
                    ? 'SPECIFIC_ACTORS' as NotificationAudienceType
                    : value.audienceType,
                targets: value.targetMode === 'SPECIFIC' ? specificTargets : undefined,
                severity: value.severity,
                kind: value.severity === 'CRITICAL' || value.severity === 'ERROR' ? 'SYSTEM_ERROR' : 'WARNING',
                category: value.category,
                titleText: value.titleFr.trim(),
                messageText: value.bodyFr.trim(),
                translations: {
                    fr: { title: value.titleFr.trim(), body: value.bodyFr.trim() },
                    en: { title: value.titleEn.trim(), body: value.bodyEn.trim() },
                    ht: { title: value.titleHt.trim(), body: value.bodyHt.trim() },
                },
                actionType: value.actionUrl.trim() ? 'LINK' : null,
                actionUrl: value.actionUrl.trim() || null,
                payload: externalDestination
                    ? { to: externalDestination, email: externalDestination, phone: externalDestination, whatsapp: externalDestination }
                    : null,
                expiresAt: null,
                channels: value.channels,
            },
            tenantId: value.targetMode === 'SPECIFIC' ? recipientSelection?.tenantId ?? null : null,
        });
    }
}
