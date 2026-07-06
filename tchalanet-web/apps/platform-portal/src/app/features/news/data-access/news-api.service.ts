import { inject, Injectable } from '@angular/core';
import type { TchRequestOptions } from '@tch/api';
import { TchBackendClient } from '@tch/api';
import type {
    ConsolePublicContentSourceType,
    ConsolePublicContentStatus,
    ConsolePublicContentSurface,
} from '@tch/web/console';
import { Observable } from 'rxjs';

export type PublicContentSourceType = ConsolePublicContentSourceType;
export type PublicContentStatus = ConsolePublicContentStatus;
export type PublicContentSurface = ConsolePublicContentSurface;

export interface PublicContentAdminItemView {
    id: string;
    title: string;
    content: string | null;
    imageUrl: string | null;
    sourceUrl: string | null;
    sourceType: PublicContentSourceType;
    status: PublicContentStatus;
    publishedAt: string | null;
    expiresAt: string | null;
    targetSurfaces: PublicContentSurface[];
    createdBy: string | null;
    createdAt: string | null;
    lastModifiedBy: string | null;
    lastModifiedAt: string | null;
}

export interface UpsertPublicContentRequest {
    id?: string | null;
    title: string;
    content?: string | null;
    contentHtml?: string | null;
    imageUrl?: string | null;
    sourceUrl?: string | null;
    status?: PublicContentStatus | null;
    targetSurfaces?: PublicContentSurface[];
    publishedAt?: string | null;
    expiresAt?: string | null;
}

@Injectable({providedIn: 'root'})
export class NewsApi {
    private readonly backend = inject(TchBackendClient);

    listNews(options?: TchRequestOptions): Observable<PublicContentAdminItemView[]> {
        return this.backend.get<PublicContentAdminItemView[]>('/platform/public-content/news', options);
    }

    upsertNews(
        payload: UpsertPublicContentRequest,
        options?: TchRequestOptions,
    ): Observable<PublicContentAdminItemView> {
        return this.backend.post<PublicContentAdminItemView>('/platform/public-content/news', payload, options);
    }

    changeNewsStatus(
        id: string,
        status: PublicContentStatus,
        options?: TchRequestOptions,
    ): Observable<PublicContentAdminItemView> {
        return this.backend.post<PublicContentAdminItemView>(
            `/platform/public-content/news/${id}/status`,
            {status},
            options,
        );
    }

    hideNews(id: string, options?: TchRequestOptions): Observable<void> {
        return this.backend.post<void>(`/platform/public-content/news/${id}/hide`, {}, options);
    }

    showNews(id: string, options?: TchRequestOptions): Observable<void> {
        return this.backend.post<void>(`/platform/public-content/news/${id}/show`, {}, options);
    }

    forceRefreshNews(options?: TchRequestOptions): Observable<void> {
        return this.backend.post<void>('/platform/public-content/news/force-refresh', {}, options);
    }
}
