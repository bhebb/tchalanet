import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';

import { SessionActions } from '@tchl/data-access/session';

@Component({
  standalone: true,
  template: `<p>Connexion en cours…</p>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthCallbackComponent implements OnInit {
  private store = inject(Store);

  ngOnInit() {
    this.store.dispatch(SessionActions.authCallbackStart());
  }
}
