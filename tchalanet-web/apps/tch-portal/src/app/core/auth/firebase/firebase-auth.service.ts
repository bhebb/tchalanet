import { inject, Injectable } from '@angular/core';
import {
  Auth,
  browserLocalPersistence,
  browserSessionPersistence,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
} from '@angular/fire/auth';

import { AuthClient, AuthLoginRequest } from '../auth-client';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService implements AuthClient {
  private readonly auth = inject(Auth);

  async isAuthenticated(): Promise<boolean> {
    await this.auth.authStateReady();
    return this.auth.currentUser !== null;
  }

  async login(request: AuthLoginRequest): Promise<void> {
    await setPersistence(
      this.auth,
      request.remember ? browserLocalPersistence : browserSessionPersistence,
    );
    await signInWithEmailAndPassword(this.auth, request.username, request.password);
  }

  async logout(): Promise<void> {
    await signOut(this.auth);
  }

  async getAccessToken(forceRefresh = false): Promise<string | null> {
    await this.auth.authStateReady();
    const currentUser = this.auth.currentUser;
    return currentUser ? currentUser.getIdToken(forceRefresh) : null;
  }

  async getTokenExpiresAt(): Promise<string | undefined> {
    await this.auth.authStateReady();
    const currentUser = this.auth.currentUser;
    if (!currentUser) {
      return undefined;
    }

    return (await currentUser.getIdTokenResult()).expirationTime;
  }
}
