import { ApiResponse, TchBackendPage, TchPage } from '../contracts/api.types';

export function unwrapApiResponse<T>(response: ApiResponse<T>): T {
  return response.data;
}

export function hasApiNotices<T>(response: ApiResponse<T>): boolean {
  return response.notices.length > 0;
}

export function normalizeTchPage<T>(
  page: TchBackendPage<T>,
  fallbackPage = 0,
  fallbackSize = 20,
): TchPage<T> {
  const items = [...(page.items ?? page.content ?? [])];
  const size = page.size ?? fallbackSize;
  const totalElements = page.totalElements ?? page.total ?? items.length;
  const pageNumber = page.page ?? page.number ?? fallbackPage;
  const totalPages = page.totalPages ?? Math.max(1, Math.ceil(totalElements / Math.max(1, size)));

  return {
    items,
    totalElements,
    totalPages,
    page: pageNumber,
    size,
    last: page.last ?? pageNumber + 1 >= totalPages,
    hasNext: page.hasNext ?? pageNumber + 1 < totalPages,
    hasPrevious: page.hasPrevious ?? pageNumber > 0,
  };
}
