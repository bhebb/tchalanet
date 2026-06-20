import { InjectionToken } from '@angular/core';

export interface AuthLoginRequest {
  readonly username: string;
  readonly password: string;
  readonly remember: boolean;
}

export interface AuthClient {
  isAuthenticated(): Promise<boolean>;
  login(request: AuthLoginRequest): Promise<void>;
  sendPasswordlessLoginLink?(email: string): Promise<void>;
  completePasswordlessLogin?(): Promise<boolean>;
  changePassword?(currentPassword: string, newPassword: string): Promise<void>;
  logout(): Promise<void>;
  getAccessToken(forceRefresh?: boolean): Promise<string | null>;
  getTokenExpiresAt(): Promise<string | undefined>;
}

export const AUTH_CLIENT = new InjectionToken<AuthClient>('AUTH_CLIENT');
