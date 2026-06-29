import { HttpParams } from '@angular/common/http';

export type QueryParamValue = string | number | boolean | null | undefined;

export type QueryParamsInput = Readonly<Record<string, QueryParamValue | readonly QueryParamValue[]>>;

export interface PageQueryInput {
  readonly page?: number | null;
  readonly size?: number | null;
  readonly sort?: string | readonly string[] | null;
}

export function toHttpParams<TInput extends object>(input: TInput = {} as TInput): HttpParams {
  let params = new HttpParams();

  Object.entries(input as QueryParamsInput).forEach(([key, rawValue]) => {
    const values = Array.isArray(rawValue) ? rawValue : [rawValue];
    values.forEach(value => {
      if (value === null || value === undefined || value === '') {
        return;
      }
      params = params.append(key, String(value));
    });
  });

  return params;
}

export function toQueryString<TInput extends object>(input: TInput = {} as TInput): string {
  return toHttpParams(input).toString();
}

export function appendQuery<TInput extends object>(path: string, input: TInput = {} as TInput): string {
  const query = toQueryString(input);
  if (!query) {
    return path;
  }
  return `${path}${path.includes('?') ? '&' : '?'}${query}`;
}

export function pageQuery(input: PageQueryInput = {}): QueryParamsInput {
  return {
    page: input.page,
    size: input.size,
    sort: input.sort,
  };
}
