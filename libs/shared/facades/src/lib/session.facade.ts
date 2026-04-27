import { inject, Injectable, signal } from '@angular/core';
import { Store } from '@ngrx/store';

// NOTE: on ne dépend pas directement de la lib session pour éviter les cycles Nx.
// On tape minimalement le state et on utilisera les clés connues.
interface SessionUser {
  preferred_username?: string;
  name?: string;
  email?: string;
}

interface SessionSlice {
  user: SessionUser | null;
  claims: unknown;
}

@Injectable({ providedIn: 'root' })
export class SessionFacade {
  private readonly store = inject<Store<{ session: SessionSlice }>>(Store as any);

  // Dérivés pratiques
  displayName = signal<string>('');
  email = signal<string | undefined>(undefined);

  constructor() {
    this.store.select(state => state.session.user).subscribe(user => {
      if (!user) {
        this.displayName.set('');
        this.email.set(undefined);
        return;
      }
      const preferred = user.preferred_username;
      const name = user.name;
      const mail = user.email;
      const label = preferred || name || mail || '';
      this.displayName.set(label);
      this.email.set(mail);
    });
  }
}
