import { createHmac, timingSafeEqual } from 'node:crypto';
import { Readable } from 'node:stream';
import type { FastifyReply, FastifyRequest, RequestPayload } from 'fastify';

import { AppError } from '../../../common/errors/app-error.js';

const DEFAULT_TOLERANCE_SECONDS = 300;
const TIMESTAMP_HEADER = 'x-tch-timestamp';
const SIGNATURE_HEADER = 'x-tch-signature';

export interface MessageHmacOptions {
  secret: string;
  toleranceSeconds?: number;
  now?: () => Date;
}

export function messageHmacPreParsing(options: MessageHmacOptions) {
  return async function verifyMessageHmac(
    request: FastifyRequest,
    _reply: FastifyReply,
    payload: RequestPayload,
  ): Promise<RequestPayload> {
    const rawBody = await readBody(payload);
    verifyHmac(request, rawBody, options);

    const stream = Readable.from(rawBody) as RequestPayload;
    stream.receivedEncodedLength = rawBody.length;
    return stream;
  };
}

function verifyHmac(request: FastifyRequest, rawBody: Buffer, options: MessageHmacOptions): void {
  if (!options.secret) {
    throw new AppError('EDGE_HMAC_SECRET is not configured', 500);
  }

  const timestamp = readHeader(request, TIMESTAMP_HEADER);
  const signature = readHeader(request, SIGNATURE_HEADER);

  if (!timestamp || !signature) {
    throw new AppError('Missing HMAC headers', 401);
  }

  assertFreshTimestamp(timestamp, options);

  const expected = sign(timestamp, rawBody, options.secret);
  if (!isEqualSignature(signature, expected)) {
    throw new AppError('Invalid HMAC signature', 401);
  }
}

async function readBody(payload: RequestPayload): Promise<Buffer> {
  const chunks: Buffer[] = [];
  for await (const chunk of payload) {
    chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
  }
  return Buffer.concat(chunks);
}

function readHeader(request: FastifyRequest, name: string): string | undefined {
  const value = request.headers[name];
  return Array.isArray(value) ? value[0] : value;
}

function assertFreshTimestamp(timestamp: string, options: MessageHmacOptions): void {
  const parsed = Date.parse(timestamp);
  if (Number.isNaN(parsed)) {
    throw new AppError('Invalid HMAC timestamp', 401);
  }

  const now = options.now?.() ?? new Date();
  const toleranceMs = (options.toleranceSeconds ?? DEFAULT_TOLERANCE_SECONDS) * 1000;
  if (Math.abs(now.getTime() - parsed) > toleranceMs) {
    throw new AppError('Expired HMAC timestamp', 401);
  }
}

function sign(timestamp: string, rawBody: Buffer, secret: string): string {
  return `sha256=${createHmac('sha256', secret)
    .update(timestamp)
    .update('.')
    .update(rawBody)
    .digest('hex')}`;
}

function isEqualSignature(actual: string, expected: string): boolean {
  const actualBuffer = Buffer.from(actual);
  const expectedBuffer = Buffer.from(expected);
  return (
    actualBuffer.length === expectedBuffer.length && timingSafeEqual(actualBuffer, expectedBuffer)
  );
}
