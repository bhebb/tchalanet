import { inject, Injectable } from '@angular/core';
import { Auth, signInWithEmailAndPassword, signOut, user } from '@angular/fire/auth';
import { firstValueFrom } from 'rxjs';

@Injectable({providedIn: 'root'})
export class FirebaseAuthService {
    private readonly auth = inject(Auth);

    readonly user$ = user(this.auth);

    async login(email: string, password: string): Promise<void> {
        await signInWithEmailAndPassword(this.auth, email, password);
    }

    async logout(): Promise<void> {
        await signOut(this.auth);
    }

    async getIdToken(forceRefresh = false): Promise<string | null> {
        const currentUser = await firstValueFrom(this.user$);
        return currentUser ? currentUser.getIdToken(forceRefresh) : null;
    }
}
