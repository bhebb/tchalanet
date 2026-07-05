import { inject, Injectable } from '@angular/core';
import type { TchRequestOptions } from '@tch/api';
import { TchBackendClient, TchPage } from '@tch/api';
import { Observable } from 'rxjs';

export type ContactRequestIntent =
    | 'REQUEST_DEMO'
    | 'BECOME_OPERATOR'
    | 'SUPPORT'
    | 'PARTNERSHIP'
    | 'OTHER';

export type ContactRequestStatus =
    | 'RECEIVED'
    | 'CONTACTED'
    | 'QUALIFIED'
    | 'CLOSED'
    | 'SPAM';

export interface ContactRequestSummaryView {
    id: string;
    reference: string;
    intent: ContactRequestIntent;
    fullName: string;
    phone: string | null;
    email: string | null;
    city: string | null;
    country: string | null;
    status: ContactRequestStatus;
    createdAt: string;
}

export interface ContactRequestAdminDetailView extends ContactRequestSummaryView {
    organizationName: string | null;
    outletCount: number | null;
    preferredContactTime: string | null;
    message: string | null;
    consentToContact: boolean;
    internalNotes: string | null;
    externalTool: string | null;
    externalReference: string | null;
    exportedAt: string | null;
    sourcePage: string | null;
    updatedAt: string;
}

@Injectable({providedIn: 'root'})
export class PlatformSupportApi {
    private readonly backend = inject(TchBackendClient);

    listContactRequests(params: {
        q?: string;
        status?: ContactRequestStatus;
        intent?: ContactRequestIntent;
        page?: number;
        size?: number;
    }, options?: TchRequestOptions): Observable<TchPage<ContactRequestSummaryView>> {
        const entries = Object.entries(params)
            .filter(([, value]) => value !== undefined && value !== null && String(value) !== '')
            .map(([key, value]) => [key, String(value)]);
        const q = new URLSearchParams(Object.fromEntries(entries)).toString();

        return this.backend.get<TchPage<ContactRequestSummaryView>>(
            `/platform/contact-requests${q ? '?' + q : ''}`,
            options,
        );
    }

    getContactRequest(id: string, options?: TchRequestOptions): Observable<ContactRequestAdminDetailView> {
        return this.backend.get<ContactRequestAdminDetailView>(`/platform/contact-requests/${id}`, options);
    }

    updateContactStatus(id: string, status: ContactRequestStatus, options?: TchRequestOptions): Observable<void> {
        return this.backend.patch<void>(`/platform/contact-requests/${id}/status`, {status}, options);
    }

    updateContactNotes(
        id: string,
        payload: {
            internalNotes: string | null;
            externalTool?: string | null;
            externalReference?: string | null;
        },
        options?: TchRequestOptions,
    ): Observable<void> {
        return this.backend.patch<void>(`/platform/contact-requests/${id}/notes`, payload, options);
    }
}
