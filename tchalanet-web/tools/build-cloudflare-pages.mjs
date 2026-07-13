import { cpSync, mkdirSync, rmSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const profile = process.argv[2] ?? 'stg-cloudflare';
const allowedProfiles = new Set(['stg-cloudflare', 'prod-cloudflare']);

if (!allowedProfiles.has(profile)) {
  console.error(`Usage: node tools/build-cloudflare-pages.mjs ${Array.from(allowedProfiles).join('|')}`);
  process.exit(1);
}

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const outDir = join(root, 'dist', 'cloudflare-pages');
const apiOrigin =
  profile === 'prod-cloudflare' ? 'https://api.tchalanet.com' : 'https://api.stg.tchalanet.com';

function pnpm(args) {
  execFileSync('pnpm', args, { cwd: root, stdio: 'inherit' });
}

function copyApp(app, targetSubdir = '') {
  const source = join(root, 'dist', 'apps', app, 'browser');
  const target = targetSubdir ? join(outDir, targetSubdir) : outDir;
  mkdirSync(target, { recursive: true });
  cpSync(source, target, { recursive: true });
}

pnpm([`runtime:${profile}`]);

rmSync(outDir, { recursive: true, force: true });
mkdirSync(outDir, { recursive: true });

pnpm(['nx', 'run', 'public-portal:build:production']);
pnpm(['nx', 'run', 'admin-portal:build:production', '--base-href=/admin/']);
pnpm(['nx', 'run', 'platform-portal:build:production', '--base-href=/platform/']);

copyApp('public-portal');
copyApp('admin-portal', 'admin');
copyApp('platform-portal', 'platform');

writeFileSync(
  join(outDir, '_redirects'),
  [
    '/admin/* /admin/index.html 200',
    '/platform/* /platform/index.html 200',
    '/* /index.html 200',
    '',
  ].join('\n'),
);

writeFileSync(
  join(outDir, '_headers'),
  [
    '/assets/i18n/*',
    '  Cache-Control: public, max-age=86400',
    '/assets/fallback/*',
    '  Cache-Control: public, max-age=86400, stale-while-revalidate=604800',
    '/*.js',
    '  Cache-Control: public, max-age=31536000, immutable',
    '/*.css',
    '  Cache-Control: public, max-age=31536000, immutable',
    '',
  ].join('\n'),
);

writeFileSync(
  join(outDir, '_worker.js'),
  `const APP_PREFIXES = ['/admin', '/platform'];
const API_ORIGIN = '${apiOrigin}';

function hasFileExtension(pathname) {
  const lastSegment = pathname.split('/').pop() ?? '';
  return lastSegment.includes('.');
}

function isApiRequest(pathname) {
  return pathname === '/api' || pathname.startsWith('/api/');
}

function proxyApiRequest(request) {
  const url = new URL(request.url);
  const target = new URL(url.pathname + url.search, API_ORIGIN);
  return fetch(new Request(target, request));
}

function rewriteSpaRequest(request) {
  if (request.method !== 'GET' && request.method !== 'HEAD') {
    return request;
  }

  const url = new URL(request.url);
  if (hasFileExtension(url.pathname)) {
    return request;
  }

  const prefix = APP_PREFIXES.find(value =>
    url.pathname === value || url.pathname.startsWith(\`\${value}/\`),
  );

  url.pathname = prefix ? \`\${prefix}/\` : '/';
  return new Request(url, request);
}

export default {
  fetch(request, env) {
    const url = new URL(request.url);
    if (isApiRequest(url.pathname)) {
      return proxyApiRequest(request);
    }

    return env.ASSETS.fetch(rewriteSpaRequest(request));
  },
};
`,
);

console.log(`Cloudflare Pages output ready: ${outDir}`);
