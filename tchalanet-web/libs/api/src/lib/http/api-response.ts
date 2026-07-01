import { ApiResponse } from '../contracts/api.types';

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  // 204 No Content / void endpoints yield a null body — return undefined instead of
  // throwing on response.data, so callers typed Observable<void> resolve cleanly.
  return response?.data as T;
}

export function hasApiNotices<T>(response: ApiResponse<T>): boolean {
  return (response?.notices?.length ?? 0) > 0;
}
