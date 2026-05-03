export type HealthStatus = 'UP' | 'DOWN';
export type ReadyStatus = 'READY' | 'NOT_READY';

export interface HealthResponse {
  status: HealthStatus;
  service: string;
}

export interface ReadyResponse {
  status: ReadyStatus;
  service: string;
}
