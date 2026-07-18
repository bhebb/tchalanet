import { HttpContextToken } from '@angular/common/http';

export type TchFeedbackOwner = 'shell' | 'page' | 'section' | 'form' | 'field' | 'feature';
export type TchFeedbackMode = 'inherit' | 'local' | 'silent';

/**
 * Declares who will render feedback for a request. Transport/interceptor code may normalize
 * feedback, but never chooses a toast, banner, panel, or field control on its own.
 */
export interface TchFeedbackContext {
  readonly owner?: TchFeedbackOwner;
  readonly mode: TchFeedbackMode;
  readonly target?: string;
}

export const TCH_FEEDBACK_CONTEXT = new HttpContextToken<TchFeedbackContext>(() => ({
  mode: 'inherit',
}));

/**
 * @deprecated Use `TCH_FEEDBACK_CONTEXT`. Kept while existing call sites migrate from a boolean
 * intent marker to explicit feedback ownership.
 */
export const SUPPRESS_SHELL_FEEDBACK = new HttpContextToken<boolean>(() => false);
