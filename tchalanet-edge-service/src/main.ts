import 'dotenv/config';

import { buildApp } from './app.js';
import { HOST, PORT } from './config/env.js';

const app = buildApp();

app.listen({ port: PORT, host: HOST }, err => {
  if (err) {
    app.log.error(err);
    process.exit(1);
  }
});
