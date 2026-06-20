import { inject, Injectable } from '@angular/core';
import {
  Auth,
  browserLocalPersistence,
  browserSessionPersistence,
  EmailAuthProvider,
  isSignInWithEmailLink,
  reauthenticateWithCredential,
  sendSignInLinkToEmail,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithEmailLink,
  signOut,
  updatePassword,
} from '@angular/fire/auth';

import { AuthClient, AuthLoginRequest } from '../auth-client';

@Injectable({ providedIn: 'root' })
export class FirebaseAuthService implements AuthClient {
  private readonly passwordlessEmailStorageKey = 'tchalanet.passwordlessLoginEmail';
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

  async sendPasswordlessLoginLink(email: string): Promise<void> {
    const normalizedEmail = email.trim();
    await sendSignInLinkToEmail(this.auth, normalizedEmail, {
      url: `${globalThis.location.origin}/login`,
      handleCodeInApp: true,
    });
    globalThis.localStorage?.setItem(this.passwordlessEmailStorageKey, normalizedEmail);
  }

  async completePasswordlessLogin(): Promise<boolean> {
    if (!isSignInWithEmailLink(this.auth, globalThis.location.href)) {
      return false;
    }

    const email = globalThis.localStorage?.getItem(this.passwordlessEmailStorageKey);
    if (!email) {
      throw new Error('Missing passwordless login email');
    }

    await signInWithEmailLink(this.auth, email, globalThis.location.href);
    globalThis.localStorage?.removeItem(this.passwordlessEmailStorageKey);
    return true;
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    await this.auth.authStateReady();
    const currentUser = this.auth.currentUser;
    if (!currentUser?.email) {
      throw new Error('No authenticated email user is available');
    }
    const credential = EmailAuthProvider.credential(currentUser.email, currentPassword);
    await reauthenticateWithCredential(currentUser, credential);
    await updatePassword(currentUser, newPassword);
    await currentUser.getIdToken(true);
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
