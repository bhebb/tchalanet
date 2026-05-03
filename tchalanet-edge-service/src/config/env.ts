export const NODE_ENV = process.env['NODE_ENV'] ?? 'development';
export const HOST = process.env['HOST'] ?? '0.0.0.0';
export const PORT = parseInt(process.env['PORT'] ?? '3000', 10);
