export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  firebaseAuthEmulatorUrl: 'http://127.0.0.1:9099',
  privateShellPolling: {
    notificationsMs: 20 * 60 * 1000,
    sessionMs: 30 * 60 * 1000,
  },
  firebase: {
    apiKey: 'demo-tchalanet-local',
    authDomain: 'demo-tchalanet-local.firebaseapp.com',
    projectId: 'demo-tchalanet-local',
    appId: 'demo-tchalanet-local',
  },
};
