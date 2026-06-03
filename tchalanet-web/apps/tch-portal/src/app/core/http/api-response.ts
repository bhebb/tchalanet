import { ApiResponse } from '../../shared/types';

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  return response.data;
}

export function hasApiNotices<T>(response: ApiResponse<T>): boolean {
  return response.notices.length > 0;
}
