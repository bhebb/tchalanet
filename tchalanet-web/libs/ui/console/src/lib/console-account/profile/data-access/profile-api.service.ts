import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { TchBackendClient } from '@tch/api';

export interface UserPreferencesView {
  readonly themeMode: 'LIGHT' | 'DARK' | 'SYSTEM' | null;
  readonly density: number | null;
  readonly locale: string | null;
  readonly timeZone: string | null;
  readonly currency: string | null;
}

export interface MeProfileResponse {
  readonly id: string | { value: string };
  readonly username: string | null;
  readonly email: string | null;
  readonly firstName: string | null;
  readonly lastName: string | null;
  readonly displayName: string | null;
  readonly roles: string[];
  readonly preferences: UserPreferencesView;
}

export interface UpdateProfileRequest {
  firstName?: string | null;
  lastName?: string | null;
  phone?: string | null;
  locale?: string | null;
}

export interface UpdatePreferencesRequest {
  themeMode?: 'LIGHT' | 'DARK' | 'SYSTEM' | null;
  density?: number | null;
  locale?: string | null;
  timeZone?: string | null;
  currency?: string | null;
}

@Injectable({ providedIn: 'root' })
export class ProfileApiService {
  private readonly backend = inject(TchBackendClient);

  getMe(): Observable<MeProfileResponse> {
    return this.backend.get<MeProfileResponse>('/tenant/me/profile');
  }

  updateProfile(req: UpdateProfileRequest): Observable<unknown> {
    return this.backend.patch<unknown>('/tenant/me/profile', req);
  }

  updatePreferences(userId: string, req: UpdatePreferencesRequest): Observable<unknown> {
    return this.backend.patch<unknown>(`/admin/identity/users/${userId}/preferences`, req);
  }

  resolveId(profile: MeProfileResponse): string {
    const id = profile.id;
    return typeof id === 'string' ? id : (id as { value: string }).value;
  }
}
