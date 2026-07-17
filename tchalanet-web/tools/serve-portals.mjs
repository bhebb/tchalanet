#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process';

const args = process.argv.slice(2);
const modeArg = args.find(arg => arg.startsWith('--mode='))?.split('=')[1] ?? 'local';
const onlyArg = args.find(arg => arg.startsWith('--only='))?.split('=')[1] ?? 'public,admin,platform';

const modes = {
  local: {
    runtime: 'local-ide',
    configuration: null,
    description: 'local IDE runtime, real Firebase config',
  },
  emulator: {
    runtime: 'local-ide-emulator',
    configuration: 'emulator',
    description: 'local IDE runtime, Firebase Auth emulator',
  },
  docker: {
    runtime: 'dev-docker',
    configuration: null,
    description: 'Docker API runtime, real Firebase config',
  },
  'docker-emulator': {
    runtime: 'dev-docker-emulator',
    configuration: 'e2e-api',
    description: 'Docker API runtime, Firebase Auth emulator',
  },
  stg: {
    runtime: 'stg-cloudflare',
    configuration: null,
    description: 'staging Cloudflare runtime profile',
  },
  prod: {
    runtime: 'prod-cloudflare',
    configuration: null,
    description: 'production Cloudflare runtime profile',
  },
};

const mode = modes[modeArg];
if (!mode) {
  console.error(`Unknown mode "${modeArg}". Use one of: ${Object.keys(modes).join(', ')}`);
  process.exit(1);
}

const allPortals = [
  ['public-portal', '4301'],
  ['admin-portal', '4302'],
  ['platform-portal', '4303'],
];
const portalAliases = new Map([
  ['public', 'public-portal'],
  ['public-portal', 'public-portal'],
  ['admin', 'admin-portal'],
  ['admin-portal', 'admin-portal'],
  ['platform', 'platform-portal'],
  ['platform-portal', 'platform-portal'],
]);

const selectedProjects = onlyArg
  .split(',')
  .map(value => value.trim())
  .filter(Boolean)
  .map(value => portalAliases.get(value));

if (selectedProjects.length === 0 || selectedProjects.some(project => !project)) {
  console.error('Invalid --only value. Use any comma-separated subset of: public, admin, platform');
  process.exit(1);
}

const selected = new Set(selectedProjects);
const portals = allPortals.filter(([project]) => selected.has(project));

console.log(`Selecting runtime profile "${mode.runtime}" (${mode.description})`);
const runtime = spawnSync(
  'node',
  ['tools/select-runtime-profile.mjs', mode.runtime],
  { stdio: 'inherit' },
);
if (runtime.status !== 0) process.exit(runtime.status ?? 1);

console.log('Starting portals:');
for (const [project, port] of portals) {
  console.log(`- ${project}: http://localhost:${port}`);
}

const children = portals.map(([project, port]) => {
  const nxArgs = ['exec', 'nx', 'run', `${project}:serve`, `--port=${port}`];
  if (mode.configuration) nxArgs.push(`--configuration=${mode.configuration}`);
  return spawn('pnpm', nxArgs, {
    stdio: 'inherit',
    env: process.env,
  });
});

let stopping = false;
function stop(signal) {
  if (stopping) return;
  stopping = true;
  for (const child of children) {
    if (!child.killed) child.kill(signal);
  }
}

process.on('SIGINT', () => stop('SIGINT'));
process.on('SIGTERM', () => stop('SIGTERM'));

for (const child of children) {
  child.on('exit', code => {
    if (!stopping && code && code !== 0) {
      stop('SIGTERM');
      process.exit(code);
    }
  });
}
