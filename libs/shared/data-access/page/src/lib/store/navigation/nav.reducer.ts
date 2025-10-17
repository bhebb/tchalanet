import { createFeature, createReducer, on } from '@ngrx/store';
import { NavAfterLoadActions } from './nav.actions';

export interface NavAfterLoadState {
  pendingTarget: string | null;
}
const initialState: NavAfterLoadState = { pendingTarget: null };

export const navAfterLoadFeature = createFeature({
  name: 'navAfterLoad',
  reducer: createReducer(
    initialState,
    on(NavAfterLoadActions.request, (s, { target }) => ({ ...s, pendingTarget: target })),
    on(NavAfterLoadActions.clear, () => initialState),
  ),
});

export const {
  name: navAfterLoadFeatureKey,
  reducer: navAfterLoadReducer,
  selectPendingTarget,
} = navAfterLoadFeature;
