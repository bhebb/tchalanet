import { PageModel } from '@tchl/types';

export interface PageState {
  page: PageModel | null;
  loading: boolean;
  error: any | null;
}

export const initialState: PageState = {
  page: null,
  loading: false,
  error: null,
};
