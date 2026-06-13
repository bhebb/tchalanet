const {
  AuthEmulator,
  SingleProjectMode,
} = require('/usr/local/lib/node_modules/firebase-tools/lib/emulator/auth');

const emulator = new AuthEmulator({
  host: '0.0.0.0',
  port: 9099,
  projectId: process.env.GCLOUD_PROJECT || 'demo-tchalanet-local',
  singleProjectMode: SingleProjectMode.ERROR,
});

async function stop(signal) {
  console.log(`Firebase Auth Emulator stopping (${signal})`);
  await emulator.stop();
  process.exit(0);
}

process.on('SIGINT', () => void stop('SIGINT'));
process.on('SIGTERM', () => void stop('SIGTERM'));

emulator
  .start()
  .then(() => console.log('Firebase Auth Emulator listening on 0.0.0.0:9099'))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
