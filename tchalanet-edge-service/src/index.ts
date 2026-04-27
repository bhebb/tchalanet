import express from 'express';
import cors from 'cors';
import morgan from 'morgan';
import dotenv from 'dotenv';
import { json } from 'express';
import { registerHealthRoutes } from './routes/health';
import { registerRuleRoutes } from './routes/rules';
import { registerEventRoutes } from './routes/events';
import { registerCommunicationRoutes } from './routes/communications';
import { registerPreviewRoutes } from './routes/preview';

dotenv.config();

const app = express();
app.use(cors());
app.use(json());
app.use(morgan('dev'));

registerHealthRoutes(app);
registerRuleRoutes(app);
registerEventRoutes(app);
registerCommunicationRoutes(app);
registerPreviewRoutes(app);

const PORT = process.env.PORT || 4001;

app.listen(PORT, () => {
  console.log(`[tchalanet-edge-service] listening on port ${PORT}`);
});
