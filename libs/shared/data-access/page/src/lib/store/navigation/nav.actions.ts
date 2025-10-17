// libs/data-access/navigation/nav.actions.ts
import { createActionGroup, emptyProps, props } from '@ngrx/store';

export const NavAfterLoadActions = createActionGroup({
  source: 'NavAfterLoad',
  events: {
    Request: props<{ target: string }>(),
    Clear: emptyProps,
  },
});
